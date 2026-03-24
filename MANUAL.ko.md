updated at 2026-03-24 by znkim

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

---

# 행성 천체 지원

## 개요

mago-3d-terrainer는 지구 외의 행성 천체에 대한 지형 타일 생성을 지원합니다. `--body` (또는 `-b`) 옵션으로 대상 천체를 지정하면, 해당 천체의 타원체 반지름과 CRS 설정이 자동으로 적용됩니다. 옵션을 생략하면 기본값인 지구가 사용되며, 기존 워크플로우는 그대로 유지됩니다.

지원 천체:
- `earth` (기본값) — WGS84 타원체
- `moon` — IAU 달 타원체 (평균 반지름 1,737,400 m, 구형)

## 달 지형 타일 생성

```
java -jar mago-3d-terrainer.jar --input "/input_path/lunar_dem" --output "/output_path/lunar_terrain" --body moon --max 8
```

## GDAL을 이용한 달 DEM 데이터 전처리

달 DEM 데이터셋(예: NASA LOLA, JAXA Kaguya/SELENE)은 일반적으로 투영 좌표계(SimpleCylindrical, 단위: 미터)로 배포됩니다. mago-3d-terrainer는 지리 좌표계(경위도, 단위: 도)를 기대하므로, 변환 전에 GDAL로 재투영해야 합니다.

**예시: NASA LRO LOLA 전구 DEM 118m**
출처: https://astrogeology.usgs.gov/search/map/moon_lro_lola_dem_118m

SimpleCylindrical에서 달 타원체 기반 경위도 좌표계로 재투영:
```
gdalwarp -t_srs "+proj=longlat +a=1737400 +b=1737400 +no_defs" -r bilinear input.tif output.tif
```

PROJ 설치 환경에 IAU 2015 데이터베이스가 포함된 경우 다음 방법도 사용 가능합니다:
```
gdalwarp -t_srs "IAU_2015:30100" input.tif output.tif
```

재투영 후 `gdalinfo`로 결과를 확인합니다. 원점이 (-180, 90)이고 픽셀 크기가 도(degree) 단위로 표시되어야 합니다:
```
Origin = (-180.000000000000000, 90.000000000000000)
Pixel Size = (0.003906250000000, -0.003906249999888)
```

> **참고:** 재투영된 GeoTIFF의 좌표계가 공식 IAU 코드 대신 `GEOGCRS["unknown"]`으로 표시될 수 있습니다. mago-3d-terrainer는 타원체 반지름 수치를 직접 비교하여 처리하므로, 별도의 CRS 수정 작업은 필요하지 않습니다.

## CesiumJS에서 렌더링

생성된 `layer.json`의 `"projection"` 값은 천체와 관계없이 `"EPSG:4326"`으로 고정됩니다. CesiumJS의 `CesiumTerrainProvider`가 지구 기반 투영 코드만 인식하기 때문입니다. 타일 격자 구조는 모든 천체에서 동일한 경위도 체계를 사용하며, 차이는 타원체 반지름뿐입니다.

달 지형을 로드할 때는 CesiumJS에서 올바른 타원체를 지정해야 합니다:

```javascript
const viewer = new Cesium.Viewer("cesiumContainer", {
    terrainProvider: await Cesium.CesiumTerrainProvider.fromUrl("/path/to/lunar_terrain", {
        ellipsoid: Cesium.Ellipsoid.MOON
    })
});
```

`ellipsoid: Cesium.Ellipsoid.MOON`을 지정하지 않으면 Cesium은 기본값인 지구 타원체를 사용하여 지형이 잘못된 스케일로 표시됩니다.

## 지리 타일링 체계

mago-3d-terrainer는 모든 천체에 대해 표준 지리 타일링 체계(경도 -180~180, 위도 -90~90)를 사용합니다. 이는 CesiumJS의 `CesiumTerrainProvider`가 내부적으로 사용하는 `GeographicTilingScheme`과 동일하므로, 행성 데이터를 위한 별도의 타일링 체계 변경은 필요하지 않습니다. GIS용으로 제공되는 행성 GeoTIFF 데이터는 대부분 -180/180 경도 체계를 따르며, 행성과학 아카이브에서 0~360 경도 체계로 배포된 데이터는 사전에 재투영해야 합니다.
