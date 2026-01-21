updated at 2026-01-09 by znkim

# 기본 변환 옵션

## 가장 간단한 데이터 변환 예시 (최소 옵션)
다음은 최소한의 옵션만 사용하여 GeoTIFF 데이터를 변환하는 간단한 예제입니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output"
```

## 최소 / 최대 타일 깊이 설정
`--minDepth <value>`와 `--maxDepth <value>` 옵션을 사용하여 타일의 최소 및 최대 깊이를 설정할 수 있습니다.  
짧은 옵션으로는 `-min <value>`와 `-max <value>`를 사용할 수 있습니다.

타일 깊이는 **0부터 시작**하며, 0은 최상위(루트) 타일을 의미합니다.  
설정 가능한 깊이 범위는 **0 ~ 22**입니다.

기본값:
- 최소 깊이: **0**
- 최대 깊이: **14**

최소 깊이는 최대 깊이보다 클 수 없습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" -max 18
```

## 타일링 상세 표현 (Intensity)
`-intensity` 또는 `-is` 옵션을 통해 타일링의 세부 표현 정도를 조절할 수 있습니다.  
intensity 값은 **1 ~ 16** 범위를 가지며, 기본값은 **4**입니다.

intensity 값을 높일수록 변환 시간이 증가하고 타일 구조가 복잡해져,  
렌더링 성능에 부정적인 영향을 줄 수 있습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --intensity 4
```

## 높이 보간 방법 설정
`-interpolationType <value>` 또는 `-it <value>` 옵션을 통해 높이 보간 방식을 설정할 수 있습니다.  
지원되는 보간 방식은 다음과 같습니다.
- `nearest`
- `bilinear` (기본값)
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --it nearest
```

## Terrain Normal 계산 (조명 처리)
`-calculateNormals` 또는 `-cn` 옵션을 사용하여 Terrain Tiles의 normal 벡터를 계산할 수 있습니다.  
기본값은 `false`입니다.

이 옵션을 활성화하면 변환 시간이 증가할 수 있으나,  
렌더링 시 광원(Lighting) 효과를 적용할 수 있습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --calculateNormals
```

## Geoid 보정 파일 사용
`--geoid <value>` 또는 `-g <value>` 옵션을 통해 Geoid 보정 모델을 적용할 수 있습니다.

정표고(Orthometric Height) 기준의 지형 데이터에  
Geoid 높이를 더하여 타원체고(Ellipsoid Height)로 변환합니다.

변환식:
```
DEM (Orthometric Height) + Geoid Height = Ellipsoid Height
```

지원 파일 형식: **GeoTIFF**  
기본 내장 Geoid 모델: **EGM96**
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "/input_path/geoid_file.tif"
```

기본 제공되는 EGM96 Geoid 모델을 사용하려면 다음과 같이 입력합니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM96"
```

---

# 변환 최적화 옵션

## 타일 레스터 최대 크기 설정
변환 과정에서 한 번에 처리할 레스터 데이터의 최대 크기를 설정합니다.  
입력 레스터가 해당 크기를 초과할 경우, 사전에 분할하여 처리합니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --rasterMaxSize 8192
```

## 타일 모자이크 크기 설정
타일링 과정에서 사용되는 레스터 버퍼(모자이크) 크기를 설정합니다.

값을 크게 설정하면 변환 속도가 소폭 향상될 수 있으나,  
메모리 사용량이 증가할 수 있습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --mosaicSize 32
```
