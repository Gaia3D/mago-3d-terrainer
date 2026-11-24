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

# 입력과 출력

## 입력 데이터
- 입력 형식: **GeoTIFF** (`.tif`, `.tiff`). 디렉터리를 입력하면 하위 폴더까지 검색합니다.
- 래스터는 투영 좌표계 또는 지리 좌표계여야 합니다. 표준화 단계에서 지리 좌표계(지구는 EPSG:4326)로 재투영됩니다.
- 래스터 값은 미터 단위 높이여야 합니다. 정표고(orthometric height)라면 `--geoid`를 사용합니다([Geoid 보정 파일 사용](#geoid-보정-파일-사용) 참고).
- 큰 래스터는 표준화 단계에서 `--rasterMaxSize` 픽셀 크기로 분할됩니다.

## 입력 데이터가 겹칠 때 (우선순위)
여러 래스터가 같은 위치를 덮으면 **픽셀 면적이 가장 작은(해상도가 가장 높은)** 래스터를 사용합니다. 그 래스터가 NODATA인 위치에서는 다음 해상도 순서의 래스터를 사용합니다.

각 depth마다 래스터를 그 depth에 맞는 해상도로 리사이즈하므로, 얕은 depth는 다운샘플된 래스터로 생성됩니다.

예를 들어 같은 해상도의 기존 래스터보다 새 측량 자료를 우선해야 할 때는 순서를 직접 지정할 수 있습니다.
- `--priorityType input` / `-pt input`: `--input` 옵션의 순서대로 우선합니다(디렉터리 입력 안의 파일은 경로순).
- `--priorityFile <file>` / `-pf <file>`: 한 줄에 래스터 파일, 디렉터리 또는 glob 패턴(`*`, `**`, `?`) 하나씩 적고, 위에 있을수록 우선합니다. 상대 경로는 우선순위 파일 기준이고, `#`으로 시작하는 줄은 주석입니다.

```
# priority.txt
D:/dem/survey-2025/*.tif
D:/dem/base-5m/
D:/dem/korea-30m.tif
```
```
java -jar mago-3d-terrainer.jar --input "D:/dem" --output "/output_path/terrain_tiles_output" --priorityFile priority.txt
```
목록에 없는 래스터는 목록에 있는 래스터 뒤에 해상도순으로 사용됩니다. 어떤 입력 래스터와도 맞지 않는 항목은 로그에 경고로 남고, 정해진 순서는 로그(`[Pre][Priority]`)에 출력됩니다.

## NODATA 값
NODATA 값(또는 NaN)과 같은 픽셀은 "데이터 없음"으로 처리됩니다. 기본값은 `-9999`입니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --nodataValue -32767
```
유효한 데이터가 전혀 없는 영역은 **타원체고 0**으로 생성됩니다. 이 영역에는 지오이드 보정이 적용되지 않으므로 해수면보다 수십 미터 낮게 표시될 수 있습니다.

참고:
- GeoTIFF의 NODATA 태그(`gdalinfo`)가 실제로 "데이터 없음"을 나타내는 값과 같은지 확인하세요. 태그는 `nan`인데 빈 영역이 `-9999`로 채워져 있다면 `--nodataValue -9999`를 지정합니다.
- 실제 높이로 쓰이는 값(예: 해안의 `0`)을 `--nodataValue`로 지정하지 마세요. 해당 높이가 모두 제거됩니다.

## 출력 구조
```
output/
├── layer.json          # 타일셋 메타데이터 (범위, 사용 가능한 타일 범위, 확장)
├── 0/0/0.terrain       # quantized-mesh 타일: {z}/{x}/{y}.terrain
├── 0/1/0.terrain
└── ...
```
- 타일은 지리 TMS 타일링 체계(`"scheme": "tms"`, `"projection": "EPSG:4326"`)를 사용하며, depth 0에 루트 타일이 2개입니다.
- `.terrain` 파일은 **gzip으로 압축되어 있지 않습니다.** 직접 압축하지 않았다면 `Content-Encoding: gzip` 헤더를 붙여 서비스하지 마세요.
- 타일 하나가 파일 하나이므로, depth가 깊은 타일셋은 파일이 수백만 개가 될 수 있습니다.

CesiumJS에서 불러오기:
```javascript
const viewer = new Cesium.Viewer("cesiumContainer", {
    terrainProvider: await Cesium.CesiumTerrainProvider.fromUrl("/path/to/terrain_tiles_output", {
        requestVertexNormals: true
    })
});
```

## 확장 (워터마스크, 메타데이터)
- `--waterMask` / `-wm`: `watermask` 확장을 추가합니다. 데이터가 없는 영역(NODATA 또는 입력 래스터 밖)을 물로 표시합니다. 타일마다 1바이트(전부 육지/전부 물) 또는 256 x 256 마스크가 저장되며, CesiumJS에서 `requestWaterMask: true`로 수면 효과에 사용됩니다.
- `--metadata` / `-md`: 타일 가용성(availability)을 담은 `metadata` 확장을 추가합니다. depth 0, 10, 20... 타일이 다음 10개 depth의 하위 타일 범위를 저장하고, layer.json에는 `"metadataAvailability": 10`과 depth 0의 `available`만 기록됩니다. CesiumJS가 가용성을 타일에서 읽으므로, 큰 타일셋에서도 layer.json이 작게 유지됩니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --waterMask --metadata
```

---

# 처리 제어

## 임시 파일
중간 타일 메시와 표준화된 래스터는 `{OUTPUT}/temp`에 저장되고 작업이 끝나면 삭제됩니다.
- `--temp <path>` / `-t <path>`: 다른 임시 폴더를 사용합니다(예: 더 빠른 디스크).
- `--leaveTemp` / `-lt`: 디버깅을 위해 임시 파일을 남깁니다.

중간 타일 메시는 힙이 허용하는 동안 메모리에 유지됩니다. 각 depth가 끝날 때(체크포인트)와 힙이 부족할 때 임시 폴더에 기록됩니다.
이때 사용하는 메모리는 JVM 옵션 `-Dmago.tempTileCacheRatio=<최대 힙 대비 비율>`로 바꿀 수 있습니다(기본값 `0.25`, `0`이면 사용 안 함).

## 중단된 작업 이어서 하기 (`--continue`)
`--continue` / `-c`는 이전 실행의 출력 폴더 타일과 임시 파일에서 작업을 이어갑니다.
```
java -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output" --continue
```
- 중단된 실행과 같은 입력과 옵션을 사용하세요.
- 출력 폴더에 있는 마지막 depth 폴더의 다음 depth부터 이어서 생성합니다.
- 이전 실행이 **depth와 depth 사이**에서 중단되었다면, 중단 없이 실행한 결과와 같습니다.
- 이전 실행이 **depth 처리 도중**에 중단되었다면, 그 depth의 나머지 타일이 생성되지 않습니다(알려진 제한 사항). 이어서 하기 전에 출력 폴더의 마지막 depth 폴더를 삭제하세요.
- 완성된 타일셋을 더 깊은 `--maxDepth`로 확장할 때도 사용할 수 있습니다. 이 경우 더 깊은 타일은 기존 quantized-mesh 타일을 바탕으로 생성됩니다.

## 타일셋 일부 수정 (`--modify`)
`--modify` / `-m`은 **아직 사용할 수 없습니다.** 현재는 옵션이 무시되고 입력 래스터로 전체를 새로 생성합니다.

기존 영역에 더 높은 해상도의 DEM을 추가하려면, 기존 DEM과 새 DEM을 함께 입력하여 타일셋을 다시 생성하세요. 겹치는 영역에서는 해상도가 높은 래스터가 사용됩니다.

## layer.json만 생성 (`--json`)
`--json` / `-j`는 타일을 생성하지 않고, 기존 타일셋에서 `layer.json`만 다시 생성합니다. 타일셋 폴더를 입력과 출력으로 지정합니다.
```
java -jar mago-3d-terrainer.jar --input "/output_path/terrain_tiles_output" --output "/output_path/terrain_tiles_output" --json
```

## 표준화 단계 건너뛰기 (`--skipStandardizationResize`)
`--skipStandardizationResize` / `-ssr`은 이전 실행이 임시 폴더에 남긴 표준화·리사이즈된 래스터를 다시 사용합니다. 예를 들어 중단 후 `--continue`와 함께 사용합니다.

## 로그
- `--log <path>` / `-l <path>`: 로그를 파일에도 기록합니다.
- `--quiet` / `-q`: 오류만 출력합니다.
- `--verbose` / `-vb`, `--debug` / `-d`: 더 자세한 로그를 출력합니다.

---

# 메모리, 디스크, 성능

## Java 힙
`-Xmx`로 최대 힙을 지정합니다. 힙이 클수록 중간 타일을 메모리에 더 많이 유지하므로 빨라집니다.
```
java -Xmx16g -jar mago-3d-terrainer.jar --input "/input_path/geotiff_folder" --output "/output_path/terrain_tiles_output"
```
여유 힙이 20% 미만이면 메모리의 중간 타일을 임시 폴더로 내립니다. 10% 미만이면 현재 타일의 메시 정제를 일찍 멈추고(로그에 `CRITICAL memory pressure`), 타일이 거칠어집니다. 이 메시지가 보이면 `-Xmx`를 늘리세요.

## 디스크 공간과 파일 수
- 타일 수는 depth마다 약 4배로 늘어납니다.
- 출력뿐 아니라 임시 파일을 위한 디스크 공간도 확보하세요. 가장 깊은 depth에서는 임시 폴더가 출력보다 클 수 있습니다([#7](https://github.com/Gaia3D/mago-3d-terrainer/issues/7) 참고).
- Windows(NTFS)에서는 작은 파일 수백만 개를 만들고 지우는 작업이 느립니다. 출력과 임시 폴더는 빠른 로컬 디스크에 두고, 정책이 허용한다면 실시간 백신 검사에서 제외하세요.

예시(`changwon_1m`, 6.5GB 1m GeoTIFF, 자동 max depth 18): 파일 476,527개, 출력 2.3GB, `-Xmx16g`에서 약 2시간.

---

# 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| `java.lang.OutOfMemoryError`, 로그에 `CRITICAL memory pressure` | 힙이 부족합니다. `-Xmx`를 늘리세요. |
| `There is not enough space on the disk` | 임시 파일에 추가 공간이 필요합니다. `--temp`로 다른 디스크를 지정하거나 공간을 확보하세요. |
| 브라우저에서 `net::ERR_CONTENT_DECODING_FAILED` | 서버가 `Content-Encoding: gzip`을 보내지만 타일은 압축되어 있지 않습니다. 헤더를 제거하거나 타일을 gzip으로 압축하세요. |
| 지형이 수십 미터 떠 있거나 가라앉음 | 입력 높이가 정표고입니다. `--geoid EGM96`/`EGM2008` 또는 지역 지오이드 파일을 사용하세요. |
| 구멍이나 평평한 영역 | 그 위치의 입력이 NODATA입니다. `--nodataValue`와 GeoTIFF의 NODATA 태그(`gdalinfo`)를 확인하세요. |
| 작업이 중단됨 | [중단된 작업 이어서 하기](#중단된-작업-이어서-하기---continue)를 참고하세요. |

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
```
gdalwarp -t_srs "IAU_2015:30100" -te -180 -90 180 90 --config CENTER_LONG 0 -r bilinear input_0_360.tif output.tif
```

> **참고:** 0~360 경도 체계나 행성 투영 좌표계로 된 입력은 아직 오류로 처리되지 않지만 결과가 잘못됩니다(서반구가 빠지거나 재투영되지 않음). 변환 전에 반드시 `gdalinfo`로 GeoTIFF를 확인하세요.

## 행성 천체의 높이 기준

내장 지오이드 모델(`EGM96`, `EGM2008`, `EGM84`)은 지구 모델입니다. `--body moon`에서는 `--geoid`를 사용하지 마세요. 달 DEM은 보통 기준 구(1,737,400 m)에 대한 높이입니다.
