![mago-3d-terrainer](https://github.com/user-attachments/assets/a36a2a8f-d031-472f-a96c-cc8d85dba686)
===

## Overview
mago 3DTerrainer is an open source based quantized-mesh terrain generator developed in Java.   
It is compatible with quantized-mesh, the native terrain data of Cesium Terrain Tiles.   
It can easily convert GeoTIFF files, the standard format of OCG, into quantized-mesh data.   
See: https://github.com/CesiumGS/quantized-mesh

![Static Badge](https://img.shields.io/badge/Gaia3D%2C%20Inc-blue?style=flat-square)
![Static Badge](https://img.shields.io/badge/QuantizedMesh-green?style=flat-square&logo=Cesium)
![Static Badge](https://img.shields.io/badge/Jdk17-red?style=flat-square&logo=openjdk)
![Static Badge](https://img.shields.io/badge/Gradle-darkorange?style=flat-square&logo=gradle)
![Static Badge](https://img.shields.io/badge/Docker%20Image-blue?style=flat-square&logo=docker)

![images2-mini](https://github.com/user-attachments/assets/be2b046a-3a79-4415-a16f-a0607976c62c)

## Key features
- Convenient conversion: convert GeoTIFF files without complicated commands.
- High accuracy: Generate quantized-mesh data with high accuracy.
- Multiple data conversion: Convert multiple GeoTIFF data at once.
- Customizable options: Provides various customization options such as min/max tile depth, tile raster max size, tile mosaic size, tile generation strength, interpolation method, etc.
- RTIN Based Terrain Simplification: utilizes the RTIN(Right-Triangulated Irregular Network) algorithm for efficient terrain simplification.

## Usage
You can download the released jar file or build the jar yourself via the mago-3d-terrainer project gradle script.   
The built jar is created in the ```/dist``` directory.

```
gradlew jar
```
###### The java version used in the release is openjdk 17.

## Example help command
```
java -jar mago-3d-terrainer.jar -help
```
Output:
```
----------------------------------------
mago-3d-terrainer(dev-version) by Gaia3D, Inc.
----------------------------------------
Usage: command options
 -h, --help                       Print Help
 -lt, --leaveTemp                 Leave temporary files for debugging
 -j, --json                       Generate layer.json from terrain data
 -c, --continue                   Continue from last terrain generation. This option can be used when terrain creation is interrupted or fails.
 -i, --input <arg>                [Required] Input directory path
 -o, --output <arg>               [Required] Output directory path
 -l, --log <arg>                  Log file path
 -t, --temp <arg>                 Temporary directory path (default: {OUTPUT}/temp)
 -g, --geoid <arg>                Set reference height option for terrain data.
                                  Geoid file path for height correction,
                                  (default: Ellipsoid)(options: Ellipsoid, EGM96 or GeoTIFF File Path)
 -min, --minDepth <arg>           Set minimum terrain tile depth
                                  (default : 0)(options: 0 - 22)
 -max, --maxDepth <arg>           Set maximum terrain tile depth
                                  (default : 14)(options: 0 - 22)
 -is, --intensity <arg>           Set Mesh refinement intensity.
                                  (default: 4.0)
 -it, --interpolationType <arg>   Set Interpolation type
                                  (default : bilinear)(options: nearest, bilinear)
 -pt, --priorityType <arg>        Nesting height priority type options
                                  (default : resolution)(options: resolution, higher)
 -nv, --nodataValue <arg>         Set NODATA value for terrain generating
                                  (default : -9999)
 -cn, --calculateNormals          Add terrain octVertexNormals for lighting effect
 -ms, --mosaicSize <arg>          Tiling mosaic buffer size per tile.
                                  (default : 16)
 -mr, --rasterMaxSize <arg>       Maximum raster size for split function.
                                  (default : 8192)
 -md, --metadata                  [Experimental] Generate metadata for the terrain data.
 -wm, --waterMask                 [Experimental] Generate water mask for the terrain data.
 -d, --debug                      [DEBUG] Print more detailed logs.
```
This is a simple Quantized-mesh conversion code with the required argument values.
```
java -jar mago-3d-terrainer-x.x.x.jar -input C:\data\geotiff-sample -output C:\data\geotiff-terrain-output -maxDepth 14
```
or
```
java -jar mago-3d-terrainer-x.x.x.x.jar -i C:\data\geotiff-sample -o C:\data\geotiff-terrain-output -max 14
```

## Using the Docker version
The mago 3DTerrainer is also available as a docker image.

#### Installation command:
```
docker pull gaia3d/mago-3d-terrainer
```
#### Running command:
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-terrainer -input /workspace/geotiff-sample -output /workspace/geotiff-terrain-output -maxDepth 14
```

## Documentation
For detailed documentation, including installation and usage instructions, please refer to the official documentation:
- JavaDocs : [gaia3d.github.io/mago-3d-terrainer](https://gaia3d.github.io/mago-3d-terrainer)
- Manual : [github.com/Gaia3D/mago-3d-terrainer](https://github.com/Gaia3D/mago-3d-terrainer/blob/main/MANUAL.md)

## Supported Java versions
Supports long-term support (LTS) versions of the JDK, including JDK17 and JDK21.

## License
- mago 3DTerrainer is licensed under the MPL2.0 license (<https://www.mozilla.org/en-US/MPL/2.0/>).
- If you do not want to release your modified code under the MPL2.0 license, you can follow a commercial license. In that case, please contact sales@gaia3d.com.

## Library dependencies
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **geotools** (Geospatial data tools library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
