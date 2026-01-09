updated at 2026-01-09 by znkim

# Basic Conversion Options

## Simple conversion example (minimum options)
The following example shows a basic conversion of GeoTIFF data using the minimum required options.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output"
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
Enable terrain normal calculation using the `-calculateNormals` or `-cn` option.  
Default value is `false`.

This increases conversion time but allows lighting effects during rendering.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --calculateNormals
```

## Using a geoid correction file
The `--geoid <value>` or `-g <value>` option applies a geoid correction model.
For terrain data stored in **orthometric height**, the geoid height is added to convert it to **ellipsoidal height**.
Formula:```DEM (Orthometric Height) + Geoid Height = Ellipsoid Height```

Supported format: **GeoTIFF**  
Built-in model: **EGM96**
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "/input_path/geoid_file.tif"
```

Using the built-in EGM96 model:
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM96"
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
