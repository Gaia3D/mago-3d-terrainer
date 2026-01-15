# Lunar Terrain Support

This document explains how multi-celestial-body support is implemented in mago-3d-terrainer, enabling terrain generation for the Moon (and potentially other bodies in the future).

## Table of Contents

1. [Quick Start](#quick-start)
2. [Input Data Requirements](#input-data-requirements)
3. [Architecture Overview](#architecture-overview)
4. [Core Components](#core-components)
5. [GeoTIFF and IAU CRS Limitations](#geotiff-and-iau-crs-limitations)
6. [Carrier CRS Strategy](#carrier-crs-strategy)
7. [Implementation Details](#implementation-details)
8. [Physical Differences: Earth vs Moon](#physical-differences-earth-vs-moon)
9. [Adding New Celestial Bodies](#adding-new-celestial-bodies)
10. [Modified Files Summary](#modified-files-summary)
11. [Testing](#testing)
12. [Troubleshooting](#troubleshooting)
13. [References](#references)

---

## Quick Start

### Earth (Default)
```bash
java -jar mago-3d-terrainer.jar -input /data/earth_dem -output /output/earth -max 14
```

### Moon
```bash
java -jar mago-3d-terrainer.jar -input /data/moon_dem -output /output/moon -body moon -max 14
```

---

## Input Data Requirements

### Geographic CRS Required

Input GeoTIFF files **must be in a Geographic Coordinate Reference System** (longitude/latitude in degrees). The application does not support projected CRS (coordinates in meters) as direct input.

#### Valid Input CRS

| CRS Type | Example | Status |
|----------|---------|--------|
| Geographic (lon/lat degrees) | WGS84, IAU:30100, custom geographic | ✅ Supported |
| Projected (meters) | Simple Cylindrical, UTM, Equidistant Cylindrical | ❌ Requires pre-processing |

#### Checking Your Data

Use `gdalinfo` to inspect your GeoTIFF:

```bash
gdalinfo your_lunar_dem.tif
```

**Good (Geographic CRS):**
```
Coordinate System is:
GEOGCRS["unknown",
    DATUM["unknown",
        ELLIPSOID["unknown",1737400,0, ...]]
    ...
    ANGLEUNIT["degree", ...]]
Corner Coordinates:
Upper Left  (-180.0, 90.0) (180d W, 90d N)
```

**Needs Pre-processing (Projected CRS):**
```
Coordinate System is:
PROJCRS["SimpleCylindrical Moon",
    ...
    LENGTHUNIT["metre",1]]
Corner Coordinates:
Upper Left  (-5458203.077, 2729101.538)  # Values in meters
```

### Pre-processing Projected Data

If your lunar DEM is in a projected CRS (like Simple Cylindrical or Equidistant Cylindrical), convert it to geographic coordinates using GDAL:

#### For Moon Data

```bash
gdalwarp -t_srs "+proj=longlat +a=1737400 +b=1737400 +no_defs" \
    input_projected.tif \
    output_geographic.tif
```

**Parameters explained:**
- `-t_srs` - Target spatial reference system
- `+proj=longlat` - Geographic projection (longitude/latitude)
- `+a=1737400` - Semi-major axis (Moon equatorial radius in meters)
- `+b=1737400` - Semi-minor axis (Moon polar radius, same as equatorial for sphere)
- `+no_defs` - Don't use default datum definitions

#### For Mars Data (Future)

```bash
gdalwarp -t_srs "+proj=longlat +a=3396190 +b=3376200 +no_defs" \
    input_projected.tif \
    output_geographic.tif
```

### Example: LRO LOLA Global DEM

The NASA LRO LOLA Global DEM is commonly distributed in Simple Cylindrical projection:

**Original (Projected):**
```
Driver: GTiff/GeoTIFF
Size is 92160, 46080
Coordinate System is:
PROJCRS["SimpleCylindrical Moon", ...]
Corner Coordinates:
Upper Left  (-5458203.077, 2729101.538)
```

**After Conversion (Geographic):**
```bash
gdalwarp -t_srs "+proj=longlat +a=1737400 +b=1737400 +no_defs" \
    Lunar_LRO_LOLA_Global_LDEM_118m_Mar2014.tif \
    Lunar_LOLA_Geographic.tif
```

```
Driver: GTiff/GeoTIFF
Size is 92160, 46080
Coordinate System is:
GEOGCRS["unknown",
    DATUM["unknown",
        ELLIPSOID["unknown",1737400,0, ...]]]
Corner Coordinates:
Upper Left  (-180.0, 90.0)
Lower Right (180.0, -90.0)
```

**Note:** You may see warnings like `PROJ: eqc: Invalid latitude` during conversion. These are normal for pole-to-pole datasets and do not affect the output quality.

### Ellipsoid Parameters

Ensure the ellipsoid parameters match the celestial body:

| Body | Semi-major axis (a) | Semi-minor axis (b) | Flattening |
|------|---------------------|---------------------|------------|
| Moon | 1,737,400 m | 1,737,400 m | 0 (sphere) |
| Mars | 3,396,190 m | 3,376,200 m | ~0.00589 |
| Earth | 6,378,137 m | 6,356,752.3 m | ~0.00335 |

---

## Architecture Overview

The lunar support implementation follows these key principles:

1. **Centralized Body Constants** - All celestial body properties (radius, eccentricity, CRS) are defined in a single `CelestialBody` enum
2. **Parameterized Coordinate Transforms** - Geographic/cartesian conversions accept a `CelestialBody` parameter
3. **Backward Compatibility** - All existing WGS84 methods continue to work unchanged (they delegate to `CelestialBody.EARTH`)
4. **No Worldfiles** - Uses a "carrier CRS" strategy to avoid external `.tfw` sidecar files

### Data Flow

```
Command Line (-body moon)
    |
    v
GlobalOptions.init() --> parses and stores CelestialBody
    |
    v
GlobalOptions.getInstance().getCelestialBody()
    |
    v
Passed to all terrain processing components:
    - GlobeUtils (coordinate conversions)
    - TileWgs84Utils (tile size calculations)
    - TerrainTriangle (mesh tessellation)
    - TileMatrix (bounding spheres, normals)
    - QuantizedMeshManager (Cesium tile export)
```

---

## Core Components

### 1. CelestialBody Enum

**Location:** `mago-common/src/main/java/com/gaia3d/util/CelestialBody.java`

The central definition of supported celestial bodies with their physical constants:

```java
@Getter
public enum CelestialBody {
    EARTH(
        "Earth",
        6378137.0,           // Equatorial radius (meters)
        6356752.3142,        // Polar radius (meters)
        6.69437999014E-3,    // First eccentricity squared
        "EPSG:4326"          // WGS84 CRS
    ),
    MOON(
        "Moon",
        1737400.0,           // Equatorial radius (meters, sphere)
        1737400.0,           // Polar radius (meters, sphere)
        0.0,                 // First eccentricity squared (perfect sphere)
        "IAU:30100"          // IAU 2015 Moon CRS
    );

    // Fields, constructor, and methods...
}
```

| Body  | Equatorial Radius | Polar Radius | Eccentricity | CRS Code |
|-------|-------------------|--------------|--------------|----------|
| EARTH | 6,378,137 m | 6,356,752.3 m | 0.00669438 | EPSG:4326 |
| MOON  | 1,737,400 m | 1,737,400 m | 0.0 (sphere) | IAU:30100 |

**Key Methods:**
- `fromString(String)` - Case-insensitive parsing ("moon", "Moon", "MOON" all work)
- `isSphere()` - Returns true for bodies with zero eccentricity
- `getEquatorialRadiusSquared()` / `getPolarRadiusSquared()` - Pre-computed for performance

### 2. GlobeUtils

**Location:** `mago-common/src/main/java/com/gaia3d/util/GlobeUtils.java`

Provides coordinate transformation utilities with body-aware overloads:

```java
// Body-parameterized (new)
GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.MOON)
GlobeUtils.cartesianToGeographic(cartesian, CelestialBody.MOON)
GlobeUtils.normalAtCartesianPoint(x, y, z, CelestialBody.MOON)

// WGS84 backward-compatible (existing - delegates to EARTH)
GlobeUtils.geographicToCartesianWgs84(lon, lat, alt)
GlobeUtils.cartesianToGeographicWgs84(cartesian)
```

#### Supported Conversions

| Method | Purpose |
|--------|---------|
| `geographicToCartesian()` | Lon/lat/alt to ECEF XYZ |
| `cartesianToGeographic()` | ECEF XYZ to lon/lat/alt |
| `normalAtCartesianPoint()` | Surface normal vector at XYZ |
| `transformMatrixAtCartesianPoint()` | Local coordinate frame at XYZ |
| `getRadiusAtLatitude()` | Radius accounting for ellipsoid |
| `radiusAtLatitudeRad()` | Radius at latitude (radians) |
| `distanceBetweenLatitudesRad()` | Arc distance in meters |
| `distanceBetweenLongitudesRad()` | Arc distance at latitude |
| `angRadLatitudeForDistance()` | Angular change for distance |
| `angRadLongitudeForDistance()` | Angular change for distance at lat |

### 3. GlobalOptions

**Location:** `mago-terrainer/src/main/java/com/gaia3d/command/GlobalOptions.java`

The singleton configuration holder includes:
- `celestialBody` field - The active body for the current run
- `outputCRS` - Automatically set based on the body's CRS code

```java
// Parse celestial body option
if (command.hasOption(CommandOptions.BODY.getLongName())) {
    String bodyValue = command.getOptionValue(CommandOptions.BODY.getLongName());
    try {
        instance.setCelestialBody(CelestialBody.fromString(bodyValue));
    } catch (IllegalArgumentException e) {
        log.warn("* Invalid celestial body: {}. Defaulting to Earth.", bodyValue);
        instance.setCelestialBody(DEFAULT_CELESTIAL_BODY);
    }
} else {
    instance.setCelestialBody(DEFAULT_CELESTIAL_BODY); // Default is EARTH
}

// Set output CRS based on celestial body
if (instance.getCelestialBody() == CelestialBody.EARTH) {
    instance.setOutputCRS(DEFAULT_TARGET_CRS); // WGS84
} else {
    try {
        String crsCode = instance.getCelestialBody().getCrsCode();
        // true = force longitude-first axis order.
        // Without this, IAU:30100 decodes with latitude-first axis order, making the
        // WGS84→IAU transform an axis-swap instead of identity, which causes lon/lat
        // to be swapped throughout the pipeline (wrong tiles, bad layer.json bounds).
        instance.setOutputCRS(CRS.decode(crsCode, true));
        log.info("Using CRS: {} for {}", crsCode, instance.getCelestialBody().getDisplayName());
    } catch (Exception e) {
        log.warn("* Failed to decode CRS for {}. Using WGS84 as carrier CRS.",
                 instance.getCelestialBody().getDisplayName());
        instance.setOutputCRS(DEFAULT_TARGET_CRS);
    }
}
```

### 4. Command Line Option

**Location:** `mago-terrainer/src/main/java/com/gaia3d/command/CommandOptions.java`

```java
BODY("body", "b", true, "Celestial body for terrain generation \n(default : earth)(options: earth, moon)")
```

---

## GeoTIFF and IAU CRS Limitations

### The Problem

The current GeoTIFF standard (v1.1) **does not support encoding IAU planetary coordinate systems**. This is a fundamental limitation of the format:

1. **Earth-centric design**: GeoTIFF was designed primarily for Earth-based coordinate reference systems in the EPSG registry. The standard assumes imagery is of Earth.

2. **No celestial body identifier**: There is currently no GeoKey to identify non-Earth bodies. A proposed GeoKey (ID 2063) would address this in future versions.

3. **Registry limitation**: GeoTIFF expects EPSG codes. Since EPSG has never adopted planetary-based definitions, IAU codes cannot be properly stored in GeoTIFF metadata.

4. **Software behavior**: While GeoTools can *read* IAU codes via the `gt-iau-wkt` plugin, the GeoTIFF *writer* throws `IllegalArgumentException: Unable to map projection` when attempting to encode IAU CRS.

### Evidence

- [OGC Testbed 19 Extraterrestrial GeoTIFF Engineering Report](https://docs.ogc.org/per/23-028.html) documents these limitations and proposes extensions
- [GeoServer IAU plugin issues](https://discourse.osgeo.org/t/native-crs-iau-2015-lunar-code-input-not-recognized-for-geotiff-layer-with-the-iau-authority-plugin/110684) show IAU_2015:30100 is not recognized in GeoTIFF layers
- [GeoTools IAU Plugin docs](https://docs.geotools.org/stable/userguide/library/referencing/iau.html) confirm these are custom codes not in EPSG

---

## Carrier CRS Strategy

### The Solution

Geographic coordinates (longitude/latitude) are **body-independent** - they represent angular positions that work on any sphere or ellipsoid. The physical meaning (actual meters on the surface) changes based on the body's radius.

Our implementation uses WGS84 as a "carrier CRS" for intermediate GeoTIFF files:

1. **If the target CRS can be encoded** (e.g., EPSG:4326), write it normally
2. **If the target CRS cannot be encoded** (e.g., IAU:30100), write using WGS84 as a "carrier CRS"
3. **The actual body context** propagates through `GlobalOptions.getCelestialBody()`

### Why This Works

- Lon/lat values are identical regardless of the CRS tag
- The body-specific radius is applied during coordinate conversions, not stored in the file
- All processing remains internal to the application
- No external `.tfw` worldfiles needed
- Works seamlessly with existing GeoTIFF tooling

### Implementation

**Location:** `mago-terrainer/src/main/java/com/gaia3d/terrain/tile/geotiff/RasterStandardizer.java`

```java
public void writeGeotiff(GridCoverage2D coverage, File outputFile) {
    try {
        // Attempt normal GeoTIFF write (via buffered stream for performance)
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        GeoTiffWriter writer = new GeoTiffWriter(new BufferedOutputStream(outputStream));
        writer.write(coverage, null);
        outputStream.flush();
        outputStream.close();
        writer.dispose();
    } catch (IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("Unable to map projection")) {
            // IAU CRS cannot be encoded - use carrier CRS
            log.warn("[Raster][I/O] IAU CRS cannot be encoded in GeoTIFF. Writing with WGS84 carrier CRS: {}",
                     outputFile.getName());
            writeGeotiffWithCarrierCrs(coverage, outputFile);
        } else {
            log.error("Failed to write GeoTiff file : {}", outputFile.getAbsolutePath());
        }
    }
}

private void writeGeotiffWithCarrierCrs(GridCoverage2D coverage, File outputFile) {
    // Create coverage with same raster data and envelope but WGS84 CRS
    GridCoverageFactory factory = new GridCoverageFactory();
    ReferencedEnvelope carrierEnvelope = new ReferencedEnvelope(
        coverage.getEnvelope2D().getMinimum(0),
        coverage.getEnvelope2D().getMaximum(0),
        coverage.getEnvelope2D().getMinimum(1),
        coverage.getEnvelope2D().getMaximum(1),
        DefaultGeographicCRS.WGS84
    );
    GridCoverage2D carrierCoverage = factory.create(
        coverage.getName(),
        coverage.getRenderedImage(),
        carrierEnvelope
    );
    // Write with standard GeoTiffWriter...
}
```

---

## Implementation Details

### Terrain Processing Updates

When processing terrain with `-body moon`, these components use body-aware calculations:

| Component | File | Change |
|-----------|------|--------|
| Tile Size | `TileWgs84Utils.java` | `body.getEquatorialRadius()` in `getTileSizeInMetersByDepth()` |
| Triangle Size | `TerrainTriangle.java` | Body-aware radius and cartesian conversion |
| Bounding Spheres | `TileMatrix.java` | Body-aware radius and normal calculations |
| Quantized Mesh | `QuantizedMeshManager.java` | Body-aware ECEF cartesian conversions |
| Envelope Metrics | `GaiaGeoTiffUtils.java` | `GlobeUtils.getRadiusAtLatitude(midLat, body)` |

### GeoTools IAU Support

**Critical Gradle Dependency:**

```gradle
// In build.gradle (root)
implementation platform("org.geotools:gt-bom:34.0")
implementation "org.geotools:gt-iau-wkt"
```

The `gt-iau-wkt` plugin enables GeoTools to decode CRS codes like `IAU:30100`. **Minimum GeoTools version: 34.0**

### CRS Comparison

**Location:** `RasterStandardizer.java`

The `isSameCRS()` method was enhanced to handle IAU CRS comparison:

```java
public boolean isSameCRS(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
    // Tier 1: identifier-based comparison (fast path)
    Iterator<ReferenceIdentifier> sourceCRSIterator = sourceCRS.getIdentifiers().iterator();
    Iterator<ReferenceIdentifier> targetCRSIterator = targetCRS.getIdentifiers().iterator();
    if (sourceCRSIterator.hasNext() && targetCRSIterator.hasNext()) {
        if (sourceCRSIterator.next().getCode().equals(targetCRSIterator.next().getCode())) {
            return true;
        }
    }

    // Tier 2: metadata-based comparison (handles IAU CRS and other edge cases)
    if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
        return true;
    }

    // Tier 3: ellipsoid comparison for unidentified geographic CRS.
    // Lunar GeoTIFFs reprojected with GDAL often lack an IAU authority identifier
    // but share the same ellipsoid axes — treat them as equivalent.
    if (!sourceCRSIterator.hasNext() && sourceCRS instanceof GeographicCRS && targetCRS instanceof GeographicCRS) {
        Ellipsoid src = ((GeographicCRS) sourceCRS).getDatum().getEllipsoid();
        Ellipsoid tgt = ((GeographicCRS) targetCRS).getDatum().getEllipsoid();
        if (Math.abs(src.getSemiMajorAxis() - tgt.getSemiMajorAxis()) < 1.0 &&
            Math.abs(src.getSemiMinorAxis() - tgt.getSemiMinorAxis()) < 1.0) {
            return true;
        }
    }
    return false;
}
```

---

## Physical Differences: Earth vs Moon

| Property | Earth | Moon | Ratio |
|----------|-------|------|-------|
| Equatorial Radius | 6,378 km | 1,737 km | ~3.67x |
| Surface Area | 510M km² | 38M km² | ~13x |
| Shape | Oblate ellipsoid | Perfect sphere | - |
| Tile Size (L14) | ~2.4 km | ~0.65 km | ~3.67x |

**Implications:**
- Same zoom level covers proportionally smaller area on the Moon
- Higher detail levels may be needed for equivalent visual resolution
- Processing time similar (fewer pixels per tile, but same algorithm)
- Quantized mesh encoding works identically (uses relative coordinates)

---

## Adding New Celestial Bodies

To add support for another body (e.g., Mars):

### 1. Add to CelestialBody enum:

```java
MARS(
    "Mars",
    3396190.0,          // equatorial radius (m)
    3376200.0,          // polar radius (m)
    0.00589,            // first eccentricity squared
    "IAU:49900"         // IAU Mars CRS
)
```

### 2. Update `fromString()` switch:

```java
return switch (value.trim().toLowerCase()) {
    case "earth" -> EARTH;
    case "moon" -> MOON;
    case "mars" -> MARS;  // Add this
    default -> throw new IllegalArgumentException("Unknown celestial body: " + value);
};
```

### 3. Update CLI help text in `CommandOptions.java`:

```java
BODY("body", "b", true, "Celestial body for terrain generation \n(default : earth)(options: earth, moon, mars)")
```

**No other code changes required** - the parameterized architecture handles everything automatically.

---

## Modified Files Summary

| File | Change |
|------|--------|
| `build.gradle` | Add `gt-iau-wkt` dependency |
| `CelestialBody.java` | **NEW** - enum in mago-common |
| `GlobeUtils.java` | Add body-parameterized overloads |
| `CommandOptions.java` | Add BODY option |
| `GlobalOptions.java` | Add celestialBody field, CRS logic |
| `RasterStandardizer.java` | Carrier CRS fallback, improved isSameCRS |
| `GaiaGeoTiffUtils.java` | Body-aware radius, target CRS check |
| `TileWgs84Utils.java` | Body-aware tile size |
| `TerrainTriangle.java` | Body-aware cartesian conversion |
| `TileMatrix.java` | Body-aware radius and normals |
| `QuantizedMeshManager.java` | Body-aware cartesian conversions |

---

## Testing

### Unit Tests

| Test File | Purpose |
|-----------|---------|
| `CelestialBodyTest.java` | Enum constants, `fromString()`, `isSphere()` |
| `GlobeUtilsBodyTest.java` | Parameterized methods, round-trip conversions |

### Integration Tests

| Test File | Purpose |
|-----------|---------|
| `MoonSupportTest.java` | Tile size ratios, coordinate consistency |

### Manual Verification

```bash
# Generate lunar terrain
java -jar mago-3d-terrainer.jar -input lunar_dem.tif -output lunar_tiles -body moon -max 8

# Verify output structure
ls lunar_tiles/  # Should contain tile hierarchy

# Check Gradle dependencies
./gradlew dependencies | grep iau  # Should show gt-iau-wkt
```

---

## Troubleshooting

### "Unknown celestial body" error
Ensure the body name is spelled correctly. Valid values: `earth`, `moon` (case-insensitive).

### IAU CRS decode failure
Verify `gt-iau-wkt` is in the classpath. Check with:
```bash
./gradlew dependencies | grep iau
```
The application will fall back to carrier CRS with a warning if decoding fails.

### Unexpected tile sizes
Confirm the correct body is selected. Moon tiles are ~3.67x smaller than Earth tiles at the same zoom level.

### "Unable to map projection" warning
This is expected for lunar terrain. The application automatically falls back to the carrier CRS strategy.

### CRS transformation failure / FactoryException
If you see errors like `Cannot find math transform` or `No transformation path found`, your input GeoTIFF is likely in a **projected CRS** (coordinates in meters) rather than a geographic CRS (coordinates in degrees).

**Solution:** Pre-process your data using GDAL:
```bash
# For Moon data
gdalwarp -t_srs "+proj=longlat +a=1737400 +b=1737400 +no_defs" input.tif output.tif
```

See [Input Data Requirements](#input-data-requirements) for details.

### Tiles missing or placed at wrong location

**Symptom:** Tiles are generated but the wrong area is covered, or the `bounds` / `available` ranges in `layer.json` don't match the input DEM extent.

**Root cause:** `CRS.decode("IAU:30100")` (without the `true` flag) returns a CRS with **latitude-first** axis order, while `DefaultGeographicCRS.WGS84` is **longitude-first**. The transform between them becomes an axis-swap instead of identity. This propagates through `GaiaGeoTiffUtils.getGeographicExtension()`, which reads the envelope as `(lat, lon)` instead of `(lon, lat)`, causing `selectTileIndicesArray()` to compute the wrong tile range.

**Fix:** `GlobalOptions.java` uses `CRS.decode(crsCode, true)` (the second `true` argument forces longitude-first axis order). If you are implementing a custom CRS path, always use the two-argument form of `CRS.decode`.

### "Bursa wolf parameters required" error
This occurs when GeoTools tries to find a strict datum transformation between the WGS84 carrier CRS and the IAU lunar CRS. The fix uses **lenient mode** (`CRS.findMathTransform(source, target, true)`) which skips Bursa-Wolf parameters. This is safe because longitude/latitude values are body-independent angular coordinates — the body-specific radius is applied separately during coordinate conversions.

### `--layerJson` standalone mode produces incorrect bounds (known bug)

When running with `--layerJson` only (without a full tile generation run), `TerrainLayer.generateAvailableTiles()` ([TerrainLayer.java:158-159](mago-terrainer/src/main/java/com/gaia3d/terrain/tile/TerrainLayer.java)) has `calcMinLat` and `calcMaxLat` computed from swapped tile indices (`lastMaxTileY` is used for the minimum, `lastMinTileY` for the maximum). This results in an inverted latitude range in `layer.json` when using standalone `--layerJson` mode. Normal tile generation is not affected.

**Workaround:** Regenerate the full tile set instead of using standalone `--layerJson`.

### Coordinates appear wrong or shifted
Verify your input GeoTIFF uses the correct ellipsoid parameters for the celestial body:
- Moon: radius = 1,737,400 m (sphere)
- Mars: equatorial = 3,396,190 m, polar = 3,376,200 m

Use `gdalinfo` to check the ellipsoid in your data matches the `-body` parameter.

---

## References

- **GeoTools IAU Plugin:** https://docs.geotools.org/stable/userguide/library/referencing/iau.html
- **OGC Extraterrestrial GeoTIFF Report:** https://docs.ogc.org/per/23-028.html
- **Quantized Mesh Specification:** https://github.com/CesiumGS/quantized-mesh
- **IAU CRS Registry:** https://voparis-vespa-crs.obspm.fr/
