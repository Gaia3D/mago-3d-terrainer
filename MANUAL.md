updated at 2026-03-24 by znkim

# Basic Conversion Options

## Simple conversion example (minimum options)
The following example shows a basic conversion of GeoTIFF data using the minimum required options.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output"
```

Multiple input files or directories can be provided by repeating the input option:
```bash
java -jar mago-3d-terrainer.jar --input "/input_path/dem-a.tif" --input "/input_path/dem-b.tif" --output "/output_path/terrain_tiles_output"
```

## Setting minimum / maximum tile depth
You can define the minimum and maximum tile depth using the `-minDepth <value>` and `-maxDepth <value>` options.  
Short options are `-min <value>` and `-max <value>`.

Tile depth starts from 0, where 0 represents the root (top-level) tile.  
Valid depth values range from **0 to 22**.

Default values:
- Minimum depth: **0**
- Maximum depth: **14**

The minimum depth must not be greater than the maximum depth.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --min 0 --max 18
```

## Tiling detail level (Intensity)
You can control the level of tiling detail using the `-intensity` or `-is` option.  
The intensity value ranges from **1 to 16**, with a default value of **4**.

Higher intensity values increase conversion time and produce more complex tiles, which may negatively impact rendering performance.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --intensity 4
```

## Height interpolation method
Use the `-interpolationType <value>` or `-it <value>` option to set the height interpolation method.  
Supported methods:
- `nearest`
- `bilinear` (default)
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --it nearest
```

## Terrain normal calculation (Lighting)
Terrain normal calculation is enabled by default and adds the `octvertexnormals` extension for lighting.

Use `--noCalculateNormals` / `-ncn` to disable normal generation when smaller output or faster conversion is preferred.  
The legacy `--calculateNormals` / `-cn` option is still accepted for compatibility, but it is no longer required.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --noCalculateNormals
```

## Using a geoid correction file
The `--geoid <value>` or `-g <value>` option applies a geoid correction model.
For terrain data stored in **orthometric height**, the geoid height is added to convert it to **ellipsoidal height**.
Formula:```DEM (Orthometric Height) + Geoid Height = Ellipsoid Height```

Supported format: **GeoTIFF**  
Built-in models: **EGM96**, **EGM2008**, **EGM84**
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "/input_path/geoid_file.tif"
```

Using the built-in EGM96 model:
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM96"
```

Using the built-in EGM2008 2.5' model:
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM2008"
```

Using the built-in EGM84 30' model:
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM84"
```

---

# Conversion Optimization Options

## Maximum raster tile size
Defines the maximum raster size processed at once.  
Larger rasters are split before processing.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --rasterMaxSize 8192
```

## Tile mosaic size
Controls the raster buffer size used during tiling.

Larger values may slightly improve performance but increase memory usage.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --mosaicSize 32
```

---

# Planetary Body Support

## Overview

mago-3d-terrainer supports terrain tile generation for planetary bodies beyond Earth. The `--body` (or `-b`) option specifies the target body and configures ellipsoid radii and CRS handling accordingly. Earth remains the default and all existing workflows are unaffected.

Supported bodies:
- `earth` (default) — WGS84 ellipsoid
- `moon` — IAU Moon ellipsoid (mean radius 1,737,400 m, spherical)

## Generating lunar terrain tiles

```
java -jar mago-3d-terrainer.jar --input "/input_path/lunar_dem" --output "/output_path/lunar_terrain" --body moon --max 8
```

## Preprocessing lunar DEM data with GDAL

Lunar DEM datasets (e.g. NASA LOLA, JAXA Kaguya/SELENE) are often distributed in a projected CRS (SimpleCylindrical, coordinates in metres). mago-3d-terrainer expects geographic coordinates (longitude/latitude in degrees). Use GDAL to reproject before conversion.

**Example: NASA LRO LOLA Global DEM 118m**
Source: https://astrogeology.usgs.gov/search/map/moon_lro_lola_dem_118m

Reproject from SimpleCylindrical to geographic lon/lat on the Moon ellipsoid:
```
gdalwarp -t_srs "+proj=longlat +a=1737400 +b=1737400 +no_defs" -r bilinear input.tif output.tif
```

Alternatively, if your PROJ installation includes the IAU 2015 database:
```
gdalwarp -t_srs "IAU_2015:30100" input.tif output.tif
```

After the warp, verify the output with `gdalinfo`. The coordinate system should show a geographic CRS with origin at (-180, 90) and pixel size in degrees:
```
Origin = (-180.000000000000000, 90.000000000000000)
Pixel Size = (0.003906250000000, -0.003906249999888)
```

> **Note:** The warped GeoTIFF may be tagged as `GEOGCRS["unknown"]` rather than a formal IAU code. mago-3d-terrainer handles this by comparing ellipsoid radii directly, so no manual CRS correction is needed.

## Rendering in CesiumJS

The generated `layer.json` uses `"projection": "EPSG:4326"` regardless of body, because CesiumJS's `CesiumTerrainProvider` only recognises Earth-based projection codes. The tile grid is the same angular lon/lat structure for all bodies; only the ellipsoid changes.

Configure CesiumJS to use the correct ellipsoid when loading lunar terrain:

```javascript
const viewer = new Cesium.Viewer("cesiumContainer", {
    terrainProvider: await Cesium.CesiumTerrainProvider.fromUrl("/path/to/lunar_terrain", {
        ellipsoid: Cesium.Ellipsoid.MOON
    })
});
```

Without `ellipsoid: Cesium.Ellipsoid.MOON`, Cesium defaults to the Earth ellipsoid and the terrain will appear at the wrong scale.

## Geographic tiling scheme

mago-3d-terrainer uses the standard geographic tiling scheme (longitude -180 to 180, latitude -90 to 90) for all bodies. This matches CesiumJS's `GeographicTilingScheme` used internally by `CesiumTerrainProvider`, so no changes to the tiling scheme are required for planetary data. Most GIS-ready planetary GeoTIFFs use this -180/180 convention; datasets in the 0–360 planetary science convention must be reprojected first.
