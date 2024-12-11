# 기본 변환 옵션
### 가장 간단한 데이터 변환 예시
다음은 최소옵션으로 geotiff 데이터를 변환하는 간단한 예제입니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output"
```

### 최소/최대 타일 깊이 설정
```-minDepth <value>```과 ```-maxDepth <value>```옵션으로 타일의 최소/최대 깊이를 지정할 수 있습니다.  
짧은 옵션은 ```-min <value>```과 ```-max <value>```입니다.

타일의 깊이는 0부터 시작하며, 0은 가장 상위 타일을 의미합니다.   
타일의 최소/최대 깊이는 (0 ~ 22)의 값을 입력할 수 있습니다.

타일 최소/최대 깊이의 기본 값은 각각 0, 14이며, 타일 최소 깊이는 타일 최대 깊이보다 작을 수 없습니다.

```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -min 0 -max 18
```

### 타일링 상세 표현
```-intensity```또는 ```-is``` 옵션을 통해 타일링의 세부 표현을 조절할 수 있습니다.   
intensity 값은 (1 ~ 16)의 값을 입력할 수 있으며, 기본 값은 4입니다.   
높은 intensity 값은 변환 시간을 증가 시키고, 복잡 해진 타일로 랜더링 퍼포먼스를 저하할 것 입니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -intensity 4
```

### 높이 보간 방법 설정
```-interpolationType <value>``` 또는 ```-it <value>``` 옵션을 통해 높이 보간 방법을 설정할 수 있습니다.   
현재 ```nearest```와 ```bilinear``` 두 가지 방법을 사용할 수 있으며, 기본 값은 ```bilinear``` 입니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -it nearest
```

### Terrain Normal 계산 (Lighting)
```-calculateNormals``` 또는 ```-cn``` 옵션을 통해 Terrain Tiles의 normal을 계산할 수 있습니다.
기본값은 ```false```이며, normal을 계산하면 변환 시간이 증가할 수 있으나 광원효과를 사용할 수 있습니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -calculateNormals
```

---
# 변환 최적화 옵션

### 타일 레스터 최대 크기 설정
타일 레스터 최대 크기를 설정하여 변환할 수 있습니다.
원본 래스터 데이터의 최대 크기를 지정합니다.
레스터 최대 크기보다 큰 레스터 데이터는 미리 분할하여 작업하게 됩니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -rasterMaxSize 8192
```

### 타일 모자이크 크기 설정
타일 모자이크 크기를 설정하여 변환할 수 있습니다.
타일 모자이크는 타일링에 필요한 레스터 타일의 버퍼 사이즈를 의미합니다.
모자이크 사이즈를 높이면 변환 속도를 단축시킬 수 있지만 메모리 사용량이 큰 폭으로 늘어납니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -mosaicSize 32
```








### 아래는 문서 참고용


### Converting General Mesh Data
Basic mesh data conversion commands that can be used when converting data.
When converting, the default coordinate system is projected to the EPSG:3857 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path" -crs 3857
```

### Batched 3D Model (b3dm)

Can be used to convert common data.
Except for point cloud data, if you do not enter an outputType, it will be generated as b3dm.

```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

### DEM elevation application case
This is the case of putting 3D data on the terrain height of a single channel such as GeoTiff.

```
java -jar mago-3d-tiler.jar -input "/input_path/sample" -output "/output_path/sample" -terrain "/input_path/sample/terrain.tif"
```

### Converting 3D data with an applied coordinate system
The example below is a sample of converting 3ds (3D MAX) data. It converts to the case where the data has a coordinate system applied to it by adding the `crs` option.
In this case, we have entered the EPSG:5186 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path/3ds" -inputType "3ds" -output "/output_path/3ds" -crs "5186"
```

---
# Converting 2D Vector Data

### Converting 2D GIS Polygon Data (Shp, GeoJson)
The example below extrudes 2D GIS polygon data.
The extrusion height can be specified to reference a customized attribute name using the `-heightColumn <arg>` attribute.
Similarly, the extrusion start height defaults to 0, and the height of the base plane can be specified via `-altitudeColumn <arg>`.

```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```
same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186" -heightAttribute "height"
```

geojson case :
```
java -jar mago-3d-tiler.jar -input "/input_path/geojson" -inputType "geojson" -output "/output_path/geojson" -crs "5186"
```

### Converting 2D GIS Polyline Data (Shp)
Convert polyline data to pipe. Polyline data with a z-axis can be converted via the `diameter` property.
The default dimension for a pipe in mago 3DTiler is diameter and The length is in millimeters (mm)
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```

---
# Converting Instance Model

### Instanced 3D Model (i3dm)

When converting instance model data, the following options are available for conversion.
(kml with collada) data, and the `outputType` option is required in the current version.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -outputType "i3dm"
```

### Converting i3dm data to Shape

To converting i3dm as a Shape file with Point geometry type, you can convert it with the following options.
You need to specify `inputType` as shp and specify the path to the instance file through the 'instance' option.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -inputType "shp" -outputType "i3dm" -instance "/input_path/instance.gltf"
```

---
# Converting Point-Clouds Data

### Converting Point-Clouds data (Point Clouds)

When converting point-clouds data, the following default options are available for conversion.
If the input data is "las", the "-outputType" will automatically be "pnts".

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las"
```

same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -outputType "pnts"
```


---
# Other examples

### Up-Axis Swap Example
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `-rotateX <degree>` option to avoid converting it.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -rotateX "90"
```

### Data flipped upside down
If the converted data is flipped upside down, add the `-rotateX <degree>` option to convert it.
```
java -jar mago-3d-tiler.jar -input "/input_path/flip-y-up-fbx" -inputType "fbx" -output "/output_path/flip-y-up-fbx" -rotateX "180"
```

### Converting CityGML
When converting to CityGML, it is recommended to give the InputType as ‘citygml’.
This is because Citygml data can have different extensions: ‘.xml’, ‘.gml’, etc.
```
java -jar mago-3d-tiler.jar -input "/input_path/citygml" -inputType "citygml" -output "/output_path/citygml" -crs "5186"
```

### Converting Large 3D Mesh Data
[Warning: Experimental]   
This option tiles large mesh data by breaking it down into smaller units.
This can be specified via the `-largeMesh` option.

```
java -jar mago-3d-tiler.jar -input "/input_path/ifc_large_mesh" -inputType "ifc" -output "/output_path/ifc_large_mesh" -largeMesh
```

### Converting Large Point-Clouds Data
When converting large point-clouds, you can use the `-pointRatio` option to adjust the percentage of conversion points from the source data as follows.

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -pointRatio "100"
```

### How to run with Docker
You can also conveniently convert to mago 3DTiler deployed on docker hub.
Pull the image of gaia3d/mago-3d-tiler and create a container.

```docker
docker pull gaia3d/mago-3d-tiler
```

Specify the input and output data paths through the workspace volume.

```docker
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -inputType 3ds -crs 5186
```