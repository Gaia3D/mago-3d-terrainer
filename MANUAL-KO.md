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
레스터 최대 크기보다 큰 레스터 데이터는 처음에 분할하여 작업하게 됩니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -rasterMaxSize 8192
```

### 타일 모자이크 크기 설정
타일 모자이크 크기를 설정하여 변환할 수 있습니다.
타일 모자이크는 타일링에 필요한 레스터 타일의 버퍼 사이즈를 의미합니다.
모자이크 사이즈를 높이면 변환 속도를 약간 단축시킬 수 있지만 메모리 사용량이 늘어납니다.
```
java -jar mago-3d-terrainer.jar -input "/input_path/geotiff_folder" -output "/output_path/terrain_tiles_output" -mosaicSize 32
```
