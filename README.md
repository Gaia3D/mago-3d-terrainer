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

## Key features
- Convenient conversion: convert GeoTIFF files without complicated commands.
- High accuracy: Generate quantized-mesh data with high accuracy.
- Multiple data conversion: Convert multiple GeoTIFF data at once.
- Customizable options: Provides various customization options such as min/max tile depth, tile raster max size, tile mosaic size, tile generation strength, interpolation method, etc.

## Usage
You can download the released jar file or build the jar yourself via the mago-3d-terrainer project gradle script.   
The built jar is created in the ```/dist``` directory.

```
gradlew jar
```
###### The java version used in the release is openjdk 17.

## Example help command
```
java -jar mago-3d-terrainer-x.x.x-shadow.jar -help
```
Output:
```
┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┏┓┳┓┳┓┏┓┳┳┓┏┓┳┓
┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┣ ┣┫┣┫┣┫┃┃┃┣ ┣┫
┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┗┛┛┗┛┗┛┗┻┛┗┗┛┛┗
3d-terrainer(dev-version) by Gaia3D, Inc.
----------------------------------------
usage: mago 3DTerrainer help
 -cn,--calculateNormals          Add terrain octVertexNormals for lighting effect
 -d,--debug                      Debug Mode, print more detail log
 -h,--help                       Print this message
 -i,--input <arg>                Input folder path
 -is,--intensity <arg>           Mesh refinement strength. (default : 4.0)
 -it,--interpolationType <arg>   Interpolation type (nearest, bilinear) (default : bilinear)
 -j,--json                       Generate only layer.json from terrain data
 -l,--log <arg>                  Log file path
 -lt,--leaveTemp                 Leave temporary files for debugging
 -max,--maxDepth <arg>           Maximum tile depth (range : 0 ~ 22) (default : 14)
 -min,--minDepth <arg>           Minimum tile depth (range : 0 ~ 22) (default : 0)
 -mr,--rasterMaxSize <arg>       Maximum raster size for split function. (default : 8192)
 -ms,--mosaicSize <arg>          Tiling mosaic buffer size per tile. (default : 16)
 -o,--output <arg>               Output folder path
 -pt,--priorityType <arg>        Priority type () (default : distance)
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
docker run --rm -v “/workspace:/workspace” gaia3d/mago-3d-terrainer -input C:\data\geotiff-sample -output C:\data\geotiff-terrain-output -maxDepth 16
```

## Supported Java versions
Supports long-term support (LTS) versions of the JDK, including JDK17 and JDK21.

## License
- mago 3DTerrainer is licensed under the MPL2.0 license (<https://www.mozilla.org/en-US/MPL/2.0/>).
- If you do not want to release your modified code under the MPL2.0 license, you can follow a commercial license. In that case, please contact sales@gaia3d.com으로.

## Library dependencies
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **geotools** (Geospatial data tools library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
