# Lunar Support Implementation Guide

This document explains how to implement lunar (and other celestial body) support in mago-3d-terrainer from scratch.

## Table of Contents

1. [Overview](#overview)
2. [Core Components](#core-components)
3. [Implementation Details](#implementation-details)
4. [Critical Points](#critical-points)
5. [Testing](#testing)

---

## Overview

The lunar support feature allows mago-3d-terrainer to generate terrain tiles for different celestial bodies, not just Earth. The implementation supports:

- **Earth** (default): WGS84 ellipsoidal model (EPSG:4326)
- **Moon**: IAU 2015 spherical model (IAU:30100)

Key differences between Earth and Moon:
- Moon radius: ~1,737.4 km (27% of Earth's radius)
- Moon is a perfect sphere (no ellipsoid)
- Different coordinate reference system (IAU vs EPSG)
- Smaller tile sizes at the same depth levels

---

## Core Components

### 1. CelestialBody Enum

**Location:** `mago-common/src/main/java/com/gaia3d/util/CelestialBody.java`

This is the central enum that defines all supported celestial bodies with their physical constants.

```java
@Getter
public enum CelestialBody {
    EARTH(
        "Earth",
        6378137.0,           // Equatorial radius in meters
        6356752.3142,        // Polar radius in meters
        6.69437999014E-3,    // First eccentricity squared
        "EPSG:4326"          // WGS84 coordinate reference system
    ),

    MOON(
        "Moon",
        1737400.0,           // Equatorial radius in meters (sphere)
        1737400.0,           // Polar radius in meters (sphere)
        0.0,                 // First eccentricity squared (perfect sphere)
        "IAU:30100"          // Native IAU 2015 CRS via gt-iau-wkt plugin
    );

    private final String name;
    private final double equatorialRadius;
    private final double polarRadius;
    private final double firstEccentricitySquared;
    private final String crsCode;

    // Constructor and utility methods...
}
```

**Key Methods:**
- `getEquatorialRadiusSquared()`: Returns radius squared for calculations
- `getPolarRadiusSquared()`: Returns polar radius squared
- `fromString(String value)`: Parse from command-line input ("earth", "moon")

### 2. Command Line Option

**Location:** `mago-terrainer/src/main/java/com/gaia3d/command/CommandOptions.java`

```java
BODY("body", "b", true, "Celestial body [earth, moon][default : earth]")
```

This adds the `-body` or `-b` option to the CLI.

### 3. Global Options Configuration

**Location:** `mago-terrainer/src/main/java/com/gaia3d/command/GlobalOptions.java`

The `GlobalOptions.init()` method parses the celestial body option and configures the CRS:

```java
// Parse celestial body option (lines 129-140)
if (command.hasOption(CommandOptions.BODY.getLongName())) {
    String bodyValue = command.getOptionValue(CommandOptions.BODY.getLongName());
    try {
        instance.setCelestialBody(CelestialBody.fromString(bodyValue));
    } catch (IllegalArgumentException e) {
        log.warn("* Invalid celestial body: {}. Defaulting to Earth.", bodyValue);
        instance.setCelestialBody(DEFAULT_CELESTIAL_BODY);
    }
} else {
    instance.setCelestialBody(DEFAULT_CELESTIAL_BODY);
}

// Set output CRS based on celestial body (lines 142-150)
try {
    String crsCode = instance.getCelestialBody().getCrsCode();
    instance.setOutputCRS(CRS.decode(crsCode));
    log.debug("Using CRS: {} for {}", crsCode, instance.getCelestialBody().getName());
} catch (Exception e) {
    log.warn("* Failed to decode CRS {}. Falling back to WGS84.",
             instance.getCelestialBody().getCrsCode(), e);
    instance.setOutputCRS(DEFAULT_TARGET_CRS);
}
```

**Important:** The CRS is automatically configured based on the selected celestial body.

---

## Implementation Details

### 1. GeoTools IAU Support

**Critical Gradle Dependency:**

```gradle
// In mago-terrainer/build.gradle
implementation "org.geotools:gt-iau-wkt:34.1"
implementation "org.geotools:gt-geotiff:34.1"
implementation "org.geotools:gt-referencing:34.1"
```

The `gt-iau-wkt` plugin is **essential** for native IAU coordinate system support. It allows GeoTools to decode CRS codes like `IAU:30100`.

**Minimum GeoTools Version:** 34+

### 2. GlobeUtils Refactoring

**Location:** `mago-common/src/main/java/com/gaia3d/util/GlobeUtils.java`

All geometric conversion methods were refactored to accept a `CelestialBody` parameter instead of using hardcoded Earth constants.

**Refactored Methods:**

#### geographicToCartesian()
```java
// Old (hardcoded Earth constants)
public static double[] geographicToCartesianWgs84(double longitude, double latitude, double altitude) {
    // Used EQUATORIAL_RADIUS, FIRST_ECCENTRICITY_SQUARED constants
}

// New (parameterized)
public static double[] geographicToCartesian(double longitude, double latitude, double altitude, CelestialBody body) {
    double e2 = body.getFirstEccentricitySquared();
    double equatorialRadius = body.getEquatorialRadius();
    double v = equatorialRadius / Math.sqrt(1.0 - e2 * sinLat * sinLat);
    // ... calculations using body parameters
}

// Backward compatibility wrapper
public static double[] geographicToCartesianWgs84(double longitude, double latitude, double altitude) {
    return geographicToCartesian(longitude, latitude, altitude, CelestialBody.EARTH);
}
```

#### radiusAtLatitudeRad()
```java
public static double radiusAtLatitudeRad(double latRad, CelestialBody body) {
    double sinLat = Math.sin(latRad);
    double e2 = body.getFirstEccentricitySquared();
    double equatorialRadius = body.getEquatorialRadius();
    return equatorialRadius / Math.sqrt(1.0 - e2 * sinLat * sinLat);
}
```

#### normalAtCartesianPoint()
```java
public static Vector3d normalAtCartesianPoint(double x, double y, double z, CelestialBody body) {
    double equatorialRadiusSquared = body.getEquatorialRadiusSquared();
    double polarRadiusSquared = body.getPolarRadiusSquared();
    Vector3d zAxis = new Vector3d(
        x / equatorialRadiusSquared,
        y / equatorialRadiusSquared,
        z / polarRadiusSquared
    );
    zAxis.normalize();
    return zAxis;
}
```

#### cartesianToGeographic()
```java
public static Vector3d cartesianToGeographic(double x, double y, double z, CelestialBody body) {
    double a = body.getEquatorialRadius();
    double e2 = body.getFirstEccentricitySquared();
    // ... conversions using body-specific parameters
}
```

**Pattern:** All new methods accept `CelestialBody body` as the last parameter. Old WGS84-specific methods are kept for backward compatibility and simply call the new methods with `CelestialBody.EARTH`.

### 3. IAU Projection Handling (WorldFile Fallback)

**Location:** `mago-terrainer/src/main/java/com/gaia3d/terrain/tile/geotiff/RasterStandardizer.java`

**Problem:** The GeoTIFF format specification partially does not support IAU coordinate reference systems. When trying to write a GeoTIFF with an IAU CRS, GeoTools throws an `IllegalArgumentException` with the message "Unable to map projection".

1. GeoTIFF format and IAU CRS: The traditional GeoTIFF specification (versions 1.0 and 1.1) was designed primarily for Earth-based coordinate reference systems, particularly those in the EPSG registry. The current GeoTIFF standard assumes that the imagery is of the Earth, and this Earth-centric view limits the use of the GeoTIFF Standard for defining locations on other celestial bodies. [OGC](https://docs.ogc.org/per/23-028.html)
2. Recent developments: However, there have been significant efforts to extend GeoTIFF support for planetary/extraterrestrial coordinate systems. OGC Testbed 19 participants recommended updating the GeoTIFF standard to support spherical and engineering coordinate systems, which are necessary to describe locations of objects in space. [OGC](https://docs.ogc.org/per/23-028.html)
3. Workarounds exist: While native support has been limited, practitioners have successfully used IAU coordinate systems in GeoTIFFs through various workarounds, such as using WKT (Well-Known Text) definitions or OGC URNs. GDAL can work with IAU coordinate systems using OGC URN format, such as "urn:ogc:def:crs:IAU2015:30100" for the Moon.

**Solution:** Implement a WorldFile fallback mechanism.

```java
private void writeGeotiffFile(GridCoverage2D coverage, File outputFile) {
    try {
        // Attempt normal GeoTIFF write
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        GeoTiffWriter writer = new GeoTiffWriter(outputFile);
        writer.write(coverage, null);
        writer.dispose();
        outputStream.close();
    } catch (IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("Unable to map projection")) {
            // IAU projection encoding failure, use WorldFile fallback
            log.warn("[Raster][I/O] GeoTIFF encoding failed for IAU projection, using WorldFile fallback: {}",
                     outputFile.getName());
            writeGeotiffWithWorldFile(coverage, outputFile);
        } else {
            throw e;
        }
    }
}
```

**WorldFile Implementation:**

```java
private void writeGeotiffWithWorldFile(GridCoverage2D coverage, File outputFile) {
    // Step 1: Extract geotransformation parameters
    GridGeometry2D gridGeometry = coverage.getGridGeometry();
    Bounds envelope = coverage.getEnvelope();
    GridEnvelope gridRange = gridGeometry.getGridRange();

    double minX = envelope.getMinimum(0);
    double maxY = envelope.getMaximum(1);

    // Step 2: Write raw TIFF image (no CRS metadata)
    RenderedImage image = coverage.getRenderedImage();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
    ImageWriter imageWriter = writers.next();

    try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
        imageWriter.setOutput(ios);
        imageWriter.write(image);
        ios.flush();
    } finally {
        imageWriter.dispose();
    }

    // Step 3: Write WorldFile (.tfw)
    File worldFile = new File(
        outputFile.getParentFile(),
        outputFile.getName().replace(".tif", ".tfw")
    );

    int width = gridRange.getSpan(0);
    int height = gridRange.getSpan(1);

    double pixelSizeX = envelope.getSpan(0) / width;
    double pixelSizeY = -envelope.getSpan(1) / height; // Negative for north-up
    double upperLeftX = minX + (pixelSizeX / 2.0);     // Center of pixel
    double upperLeftY = maxY + (pixelSizeY / 2.0);

    // WorldFile format (6 lines):
    try (PrintWriter tfwWriter = new PrintWriter(worldFile)) {
        tfwWriter.println(String.format(Locale.US, "%.10f", pixelSizeX));    // Line 1: X pixel size
        tfwWriter.println("0.0");                                              // Line 2: X rotation
        tfwWriter.println("0.0");                                              // Line 3: Y rotation
        tfwWriter.println(String.format(Locale.US, "%.10f", pixelSizeY));    // Line 4: Y pixel size (negative)
        tfwWriter.println(String.format(Locale.US, "%.10f", upperLeftX));    // Line 5: X coordinate of upper-left
        tfwWriter.println(String.format(Locale.US, "%.10f", upperLeftY));    // Line 6: Y coordinate of upper-left
    }
}
```

**WorldFile Format Explanation:**
- **Line 1:** X-direction pixel size (degrees per pixel)
- **Line 2:** Rotation about Y-axis (typically 0.0)
- **Line 3:** Rotation about X-axis (typically 0.0)
- **Line 4:** Y-direction pixel size (negative for north-up images)
- **Line 5:** X-coordinate of the center of the upper-left pixel
- **Line 6:** Y-coordinate of the center of the upper-left pixel

### 4. CelestialBody Propagation

The selected `CelestialBody` must be accessible throughout the application. Here's how it flows:

```
Command Line (-body moon)
    ↓
GlobalOptions.init() → parses and stores CelestialBody
    ↓
GlobalOptions.getInstance().getCelestialBody()
    ↓
Passed to:
    - TileWgs84Manager
    - TerrainElevationDataManager
    - TerrainMesh calculations
    - All GlobeUtils methods
```

**Example propagation in tile processing:**

```java
public class TileWgs84Manager {
    private CelestialBody celestialBody;

    public void initialize() {
        this.celestialBody = GlobalOptions.getInstance().getCelestialBody();
    }

    public void processGeometry() {
        // Use celestial body in calculations
        double[] cartesian = GlobeUtils.geographicToCartesian(
            longitude, latitude, altitude, celestialBody
        );
    }
}
```

---

## Critical Points

### 1. GeoTools Version Requirement

**Minimum:** GeoTools 34.0 or higher

GeoTools 34+ introduced native IAU authority support through the `gt-iau-wkt` plugin. Earlier versions do not support IAU coordinate systems.

### 2. WorldFile Fallback

GeoTIFF format cannot encode IAU projections internally. The WorldFile (`.tfw`) sidecar approach is one of the way to preserve georeferencing for lunar data.

**Files generated for lunar terrain:**
- `tile.tif` - Raw image data
- `tile.tfw` - Georeferencing parameters

### 3. Moon is a Perfect Sphere

Unlike Earth, the Moon has no ellipsoidal flattening:
- `equatorialRadius = polarRadius = 1,737,400 meters`
- `firstEccentricitySquared = 0.0`

This simplifies many calculations but must be handled correctly in the code.

### 4. Tile Size Differences

Due to the Moon's smaller radius (~27% of Earth), tiles at the same depth level are proportionally smaller:

| Depth | Earth Tile Size | Moon Tile Size |
|-------|----------------|----------------|
| 0     | 180° × 180°    | 180° × 180°    |
| 14    | ~11 km         | ~3 km          |
| 18    | ~0.7 km        | ~0.19 km       |

This affects:
- Processing time (more tiles needed for same ground coverage)
- Detail levels achievable at different depths
- Memory usage

---

## Testing

### Test File Location

`mago-terrainer/src/test/java/com/gaia3d/release/others/MoonSupportTest.java`

### Key Test Cases

1. **CelestialBody Enum Tests:**
   - Verify radius values
   - Test `fromString()` parsing
   - Validate CRS codes

2. **GlobeUtils Tests:**
   - Geographic to Cartesian conversion (Earth vs Moon)
   - Radius at latitude calculations
   - Normal vector calculations
   - Cartesian to Geographic conversion

3. **Integration Tests:**
   - Process lunar GeoTIFF data
   - Verify WorldFile generation
   - Check tile size calculations
   - Validate output terrain tiles

---

## Usage Examples

### Basic Earth Terrain (Default)

```bash
java -jar mago-3d-terrainer.jar \
  -input /data/earth_geotiffs \
  -output /output/earth_terrain \
  -max 14
```

### Basic Moon Terrain

```bash
java -jar mago-3d-terrainer.jar \
  -input /data/moon_geotiffs \
  -output /output/moon_terrain \
  -body moon \
  -max 14
```

---

## References

- **GeoTools IAU Support:** https://docs.geotools.org/
- **Quantized Mesh Specification:** https://github.com/CesiumGS/quantized-mesh
- **WorldFile Format:** https://en.wikipedia.org/wiki/World_file
