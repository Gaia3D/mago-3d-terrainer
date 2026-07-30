# 기본 변환 옵션

## 가장 간단한 데이터 변환 예시 (최소 옵션)
다음은 최소한의 옵션만 사용하여 GeoTIFF 데이터를 변환하는 간단한 예제입니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output"
```

여러 입력 파일 또는 디렉터리는 `--input` 옵션을 반복해서 지정할 수 있습니다.
```bash
java -jar mago-3d-terrainer.jar --input "/input_path/dem-a.tif" --input "/input_path/dem-b.tif" --output "/output_path/terrain_tiles_output"
```

## 최소 / 최대 타일 깊이 설정
`--minDepth <value>`와 `--maxDepth <value>` 옵션을 사용하여 타일의 최소 및 최대 깊이를 설정할 수 있습니다.  
짧은 옵션으로는 `-min <value>`와 `-max <value>`를 사용할 수 있습니다.

타일 깊이는 **0부터 시작**하며, 0은 최상위(루트) 타일을 의미합니다.  
설정 가능한 깊이 범위는 **0 ~ 22**입니다.

기본 동작:
- 최소 깊이는 사실상 **0**으로 고정됩니다.
- 최대 깊이를 생략하면 입력 래스터 해상도를 기준으로 자동 계산됩니다.

최소 깊이는 최대 깊이보다 클 수 없습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --min 0 --max 18
```

## 타일 깊이와 원본 래스터 해상도 가이드
각 지형 타일은 남북 방향으로 `180 / 2^depth` 도를 차지합니다. 아래 표는 기본값인 지구 기준에서 타일 하나의 대략적인 남북 길이를 보여줍니다. 변환기에서 사용하는 지구 적도 반지름과 같은 값을 기준으로 계산했으므로, 정밀 측지 거리표라기보다 `maxDepth` 선택을 위한 실무 가이드로 보면 됩니다.

달 지형의 경우 아래 길이에 약 **0.2724**(`1,737,400 / 6,378,137`)를 곱해 참고하면 됩니다.

| Depth | 타일 하나의 남북 길이 (km) | 타일 하나의 남북 길이 (m) |
| ---: | ---: | ---: |
| 0 | 20,037.508 | 20,037,508 |
| 1 | 10,018.754 | 10,018,754 |
| 2 | 5,009.377 | 5,009,377 |
| 3 | 2,504.689 | 2,504,689 |
| 4 | 1,252.344 | 1,252,344 |
| 5 | 626.172 | 626,172 |
| 6 | 313.086 | 313,086 |
| 7 | 156.543 | 156,543.034 |
| 8 | 78.272 | 78,271.517 |
| 9 | 39.136 | 39,135.758 |
| 10 | 19.568 | 19,567.879 |
| 11 | 9.784 | 9,783.940 |
| 12 | 4.892 | 4,891.970 |
| 13 | 2.445985 | 2,445.985 |
| 14 | 1.222992 | 1,222.992 |
| 15 | 0.611496 | 611.496 |
| 16 | 0.305748 | 305.748 |
| 17 | 0.152874 | 152.874 |
| 18 | 0.076437 | 76.437 |
| 19 | 0.038219 | 38.219 |
| 20 | 0.019109 | 19.109 |
| 21 | 0.009555 | 9.555 |
| 22 | 0.004777 | 4.777 |

GeoTIFF에 적절한 `maxDepth`를 가늠하는 방법은 다음과 같습니다.
- `gdalinfo` 또는 GIS 도구로 원본 래스터의 픽셀 크기를 확인합니다.
- 픽셀 크기에 **256**을 곱해 256 샘플 타일 하나가 표현하는 지상 길이를 추정합니다.
- 위 표에서 그 길이에 가까운 depth를 선택합니다. 자동 `maxDepth` 계산도 이 기준을 따르며, 타일 길이가 `픽셀 크기 * 256`보다 작아지는 지점에서 보통 한 단계 더 세밀한 depth를 선택합니다.

실무 예시는 다음과 같습니다.

| GeoTIFF 픽셀 크기 | 256 픽셀의 지상 길이 | 일반적인 `maxDepth` |
| ---: | ---: | ---: |
| 90 m | 23,040 m | 11 |
| 30 m | 7,680 m | 13 |
| 10 m | 2,560 m | 14 |
| 5 m | 1,280 m | 15 |
| 1 m | 256 m | 18 |

원본 래스터가 지원하는 해상도보다 훨씬 큰 `maxDepth`를 지정하면 실제 지형 디테일은 늘지 않고 타일 수, 처리 시간, 저장 용량만 증가합니다. 변환 속도나 결과물 크기를 줄여야 할 때는 더 작은 `maxDepth`를 사용합니다.

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
Terrain normal 계산은 기본적으로 활성화되며, 조명을 위한 `octvertexnormals` 확장을 생성합니다.

normal 생성을 원하지 않는 경우 `--noCalculateNormals` / `-ncn` 옵션을 사용합니다.  
기존 호환성을 위해 `--calculateNormals` / `-cn` 옵션은 계속 허용되지만, 더 이상 지정할 필요는 없습니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --noCalculateNormals
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
기본 내장 Geoid 모델: **EGM96**, **EGM2008**, **EGM84**
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "/input_path/geoid_file.tif"
```

기본 제공되는 EGM96 Geoid 모델을 사용하려면 다음과 같이 입력합니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM96"
```

기본 제공되는 EGM2008 2.5' Geoid 모델을 사용하려면 다음과 같이 입력합니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM2008"
```

기본 제공되는 EGM84 30' Geoid 모델을 사용하려면 다음과 같이 입력합니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --geoid "EGM84"
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
