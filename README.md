mago 3DTerrainer
===

### Overview:
mago 3DTerrainer is an open source based quantized-mesh terrain converter developed in Java.   
It is compatible with quantized-mesh, the native terrain data of Cesium Terrain Tiles.   
It can easily convert GeoTIFF files, the standard format of OCG, into quantized-mesh data.   
See: https://github.com/CesiumGS/quantized-mesh

### Key features:
- Convenient conversion: convert GeoTIFF files without complicated commands.
- High accuracy: Generate quantized-mesh data with high accuracy.
- Multiple data conversion: Convert multiple GeoTIFF data at once.
- Customizable options: Provides various customization options such as min/max tile depth, tile raster max size, tile mosaic size, tile generation strength, interpolation method, etc.

### Usage:
By default, you can build the runnable jar through the gradle script of the mago-3d-terrainer project when modifying the code.   
In the /mago-3d-terrainer/dist/ directory, there are pre-built jars for each version.
- mago-3d-terrainer-1.X.X.jar

The java version used in the build is openjdk 17:**.

Below is an example of running the Help code.
```
java -jar mago-3d-terrainer-x.x.x-shadow.jar -h
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
 -is,--intensity <arg>           Mesh refinement strength. (default : 2.0)
 -it,--interpolationType <arg>   Interpolation type (nearest, bilinear) (default : bilinear)
 -j,--json                       Generate only layer.json from terrain data
 -l,--log <arg>                  Log file path
 -max,--maxDepth <arg>           Maximum tile depth (range : 0 ~ 22) (default : 14)
 -min,--minDepth <arg>           Minimum tile depth (range : 0 ~ 22) (default : 0)
 -mr,--rasterMaxSize <arg>       Maximum raster size for split function. (default : 8192)
 -ms,--mosaicSize <arg>          Tiling mosaic buffer size per tile. (default : 32)
 -o,--output <arg>               Output folder path
```
This is a simple Quantized-mesh conversion code with the required argument values.
```
java -jar mago-3d-terrainer-x.x.x.jar --input C:\data\geotiff-sample --output C:\data\geotiff-terrain-output --maxDepth 14
```
or
```
java -jar mago-3d-terrainer-x.x.x.x.jar -i C:\data\geotiff-sample -o C:\data\geotiff-terrain-output -max 14
```

### Using the Docker version:
The mago 3DTerrainer is also available as a docker image.

Example usage:
```
docker pull gaia3d/mago-3d-terrainer
```
```
docker run --rm -v “/workspace:/workspace” gaia3d/mago-3d-terrainer -i C:\data\geotiff-sample -o C:\data\geotiff-terrain-output -max 16
```

### Supported Java versions:
Supports compatibility with LTS (Long-term support) versions of the JDK, such as JDK17, JDK21, etc.

### License:
- mago 3DTerrainer is licensed under the MPL2.0 license (<https://www.mozilla.org/en-US/MPL/2.0/>).
- If you do not want to release your modified code under the MPL2.0 license, you can follow a commercial license. In that case, please contact sales@gaia3d.com으로.

### Library dependencies:
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **geotools** (Geospatial data library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>

Translated with DeepL.com (free version)

---

mago 3DTerrainer 
===

### 개요:
mago 3DTerrainer는 자바로 개발된 오픈소스 기반의 quantized-mesh terrain 변환기입니다.   
Cesium Terrain Tiles의 기본 Terrain 데이터인 quantized-mesh와 호환됩니다.   
OCG의 표준 포맷인 GeoTIFF 파일을 공간정보 레스터 데이터를 손쉽게 quantized-mesh 데이터로 변환할 수 있습니다.   
참고 : https://github.com/CesiumGS/quantized-mesh

### 주요 기능:
- 편리한 변환: GeoTIFF 파일을 복잡한 커맨드 없이 변환할 수 있습니다.
- 높은 정확도: 높은 정확도의 quantized-mesh 데이터를 생성합니다.
- 다수의 데이터 변환: 다수의 GeoTIFF 데이터를 한 번에 변환할 수 있습니다.
- 상세옵션 조절: 최소/최대 타일 깊이, 타일 레스터 최대 크기, 타일 모자이크 크기, 타일생성 강도, 보간방법 등 다양한 상세옵션을 제공합니다.

### 사용법:
기본적으로 코드 수정 시 mago-3d-terrainer 프로젝트의 gradle script를 통해 runnable jar를 빌드할 수 있습니다.   
/mago-3d-terrainer/dist/ 디렉토리에는 매 버전에 따라 미리 빌드된 jar가 준비 되어있습니다.
- mago-3d-terrainer-1.X.X.jar

빌드 시 사용된 java 버전은 openjdk 17 입니다.**

아래는 Help 코드를 실행시킨 예시입니다.
```
java -jar mago-3d-terrainer-x.x.x-shadow.jar -h
```
출력 결과물:
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
 -is,--intensity <arg>           Mesh refinement strength. (default : 2.0)
 -it,--interpolationType <arg>   Interpolation type (nearest, bilinear) (default : bilinear)
 -j,--json                       Generate only layer.json from terrain data
 -l,--log <arg>                  Log file path
 -max,--maxDepth <arg>           Maximum tile depth (range : 0 ~ 22) (default : 14)
 -min,--minDepth <arg>           Minimum tile depth (range : 0 ~ 22) (default : 0)
 -mr,--rasterMaxSize <arg>       Maximum raster size for split function. (default : 8192)
 -ms,--mosaicSize <arg>          Tiling mosaic buffer size per tile. (default : 32)
 -o,--output <arg>               Output folder path
```

필수 인자 값으로 작성한 간단한 Quantized-mesh 변환코드 입니다.
```
java -jar mago-3d-terrainer-x.x.x.jar --input C:\data\geotiff-sample --output C:\data\geotiff-terrain-output --max 14
```
또는
```
java -jar mago-3d-terrainer-x.x.x.jar -i C:\data\geotiff-sample -o C:\data\geotiff-terrain-output -max 14
```

### 도커 버전 사용법:
mago 3DTerrainer는 도커 이미지로도 사용이 가능합니다.

사용 예시:
```
docker pull gaia3d/mago-3d-terrainer
```
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-terrainer -i C:\data\geotiff-sample -o C:\data\geotiff-terrain-output -max 14
```

### 지원하는 자바 버전:
JDK17, JDK21 등 JDK 의 LTS(Long-term support) 버전의 호환을 지원합니다.

### 라이선스:
- mago 3DTerrainer는 MPL2.0 라이선스를 따릅니다. (<https://www.mozilla.org/en-US/MPL/2.0/>)
- 만약 MPL2.0라이선스에 따라 여러분이 개작, 수정한 코드를 공개하고 싶지 않으면 상업 라이선스를 따르시면 됩니다. 이 경우에는 sales@gaia3d.com으로 연락 주시기 바랍니다.

### 라이브러리 의존성:
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **geotools** (Geospatial data library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>