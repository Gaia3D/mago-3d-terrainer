# Default transformation options
### Simplest data conversion example
The following is a simple example of converting geotiff data with minimal options.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output”
```

### Set minimum/maximum tile depth
You can specify the minimum/maximum depth of the tiles with the options ```-minDepth <value>``` and ```-maxDepth <value>```.  
The shorter options are ```-min <value>``` and ```-max <value>```.

The depth of the tiles starts at 0, where 0 is the topmost tile.   
You can enter a value between (0 and 22) for the minimum and maximum depth of a tile.

The default values for tile min and max depth are 0 and 14, respectively, and the tile min depth cannot be less than the tile max depth.

```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -min 0 -max 18
```

### Tiling details
The ```-intensity``` or ```-is``` options allow you to control the detail of the tiling.   
The intensity value can be any value from (1 to 16), with a default value of 4.   
Higher intensity values will increase conversion time and decrease rendering performance with more complex tiles.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -intensity 4
```

### Set the height interpolation method
You can set the height interpolation method via the ```-interpolationType <value>``` or ```-it <value>``` options.   
Currently, two methods are available: ```nearest``` and ```bilinear```, with the default being ```bilinear```.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -it nearest
```

### Calculate Terrain Normal (Lighting)
The ```-calculateNormals``` or ```-cn``` options allow you to calculate the normals for Terrain Tiles.
The default is ```false```, which allows you to use lighting effects, although calculating normals may increase conversion time.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -calculateNormals
```

---
# conversion optimization options

### Set tile register maximum size
You can set the tile register maximum size to optimize the conversion.
Specify the maximum size of the original raster data.
Raster data larger than the raster max size will be initially split.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -rasterMaxSize 8192
```

### Setting the tile mosaic size
You can set the tile mosaic size to convert.
The tile mosaic is the size of the buffer of raster tiles needed for tiling.
Increasing the mosaic size can slightly speed up the conversion, but at the cost of increased memory usage.
```
java -jar mago-3d-terrainer.jar -input “/input_path/geotiff_folder” -output “/output_path/terrain_tiles_output” -mosaicSize 32
```