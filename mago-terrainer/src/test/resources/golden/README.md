# Release feature tests (golden files)

`com.gaia3d.release.ReleaseFeatureTest` runs each feature of the terrainer on a small sample and compares the output with a golden manifest in this folder.

```bash
# run (about 1.5 ~ 3 min)
./gradlew :mago-terrainer:test -PincludeReleaseTests --tests "com.gaia3d.release.ReleaseFeatureTest"

# rewrite the golden manifests (only when the output change is intended, review the diff before committing)
./gradlew :mago-terrainer:test -PincludeReleaseTests -PupdateGolden --tests "com.gaia3d.release.ReleaseFeatureTest"
```

The outputs and logs are written to `mago-terrainer/build/release-test/{case}`.

## Manifest format

`{case}.manifest` has one line per file: `{relative path} {SHA-256}`, for every `.terrain` tile and `layer.json`.

- The tiles are hashed in a canonical form: the 4 edge vertex index lists are sorted, because their order is not meaningful.
  (The order of the edge indices of the antimeridian tiles `0/0/0` and `0/1/0` is not deterministic.)
- A failure reports the missing, different and extra files. Compare `build/release-test/{case}` with a previous output to see what changed.

## Cases

| Case | Feature | Input | Options |
|---|---|---|---|
| F01 | basic (EPSG:5186, bilinear, normals) | `sample` | `-max 10` |
| F02 | nearest interpolation | `sample` | `-max 10 -it nearest` |
| F03 | no normals | `sample` | `-max 10 -ncn` |
| F04 | refinement intensity | `sample` | `-max 10 -is 8` |
| F05 | mosaic size, raster split size | `sample` | `-max 10 -ms 4 -mr 1024` |
| F06 | water mask, metadata extensions | `sample` | `-max 10 -wm -md`, the tile hashes include the extensions, layer.json has `metadataAvailability` (requires #44) |
| F07 | built-in geoid | `sample` | `-max 10 -geoid EGM96` |
| F08 | external geoid file | `sample` | `-max 10 -geoid kr_ngii_KNGeoid18_4326.tif` |
| F09 | EPSG:5186 5m input | `another-crs-sample` | `-max 12` |
| F10 | EPSG:4326 input, separated files | `copernicus-sample` | `-max 9` |
| F11 | EPSG:5187 1m input, automatic max depth | `precision-test-garisan/garisan-precision-1m.tif` | (none) |
| F12 | multiple adjacent files | `multi-sample` | `-max 12` |
| F13 | multi-resolution overlap (500m/100m/30m, with -9999 holes), resolution priority | `multi-resolution-sample` | `-max 12` |
| F14 | multi-resolution overlap, priority file (100m, 500m, then the unlisted 30m) | `multi-resolution-sample` | `-max 12 -priorityFile priority.txt`, and the same order with `-pt input` (requires #29) |
| F15 | repeated `-input`, NODATA value | `dem500.tif`, `dem100.tif` | `-max 11 -nv 0` |
| F16 | leave temp | `sample` | `-max 10 -lt` → same as F01, temp kept |
| F17 | custom temp path | `sample` | `-max 10 -t {dir}` → same as F01 |
| F18 | `layer.json` only | F18 output (`-max 8`) | `-json` |
| F19 | continue a completed tileset to a deeper depth | `sample` | `-max 8`, then `-max 10 -continue` |
| F20 | recovery after a crash at a depth boundary | `sample` | child JVM killed after depth 7, then `-continue` → same as F01 |
| F21 | recovery after a crash in the middle of a depth | `multi-resolution-sample` | child JVM killed in the 2nd subrange of depth 11, then `-continue` → same files as F13, the depth 11 is generated again from the quantized meshes (requires #34) |
| F22 | moon, global DEM in -180..180 | `moon-sample` (`moon_ldem4_geo_pm180.tif`) | `-body moon -max 5` |
| F23 | moon, 0..360 longitudes | `moon_ldem4_geo_0_360.tif` | same as F22 (shifted and rotated into -180..180) |
| F24 | moon, PDS projected CRS (`SIMPLE_CYLINDRICAL MOON`) | `moon_ldem4_eqc_0_360.tif` | same files as F22, own golden (reprojected) |
| F25 | moon with an Earth geoid | `moon_ldem4_geo_pm180.tif` | `-geoid EGM96` is rejected |

Not covered yet: very large inputs (see `DefaultReleaseTest`, `BuildTest` with the local release-sample data).

## Sample data

`multi-resolution-sample/input` was cropped from `release-sample/multi-resolution` (EPSG:5186) with GDAL:

```bash
gdal_translate -projwin 300000 300000 395000 243000 -co COMPRESS=DEFLATE -co PREDICTOR=2 -co TILED=YES dem500-5186-crop.tif dem500.tif
gdal_translate -projwin 320000 285000 375000 245000 -co COMPRESS=DEFLATE -co PREDICTOR=3 -co TILED=YES dem100-5186-mask.tif dem100.tif
gdal_translate -projwin 335000 270000 355000 250000 -co COMPRESS=DEFLATE -co PREDICTOR=3 -co TILED=YES dem30-mini-5186-part.tif dem30.tif
```

`moon-sample/input` is the LRO LOLA GDR `ldem_4` (4 pixels per degree, NASA PDS public data, [PDS Geosciences Node](https://pds-geosciences.wustl.edu/lro/lro-l-lola-3-rdr-v1/lrolol_1xxx/data/lola_gdr/cylindrical/img/)) converted with GDAL:

```bash
gdal_translate -of VRT -a_scale 0.5 -a_offset 0 ldem_4.lbl tmp_scaled.vrt
gdal_translate -unscale -ot Float32 -a_nodata -32768 -co COMPRESS=DEFLATE tmp_scaled.vrt moon_ldem4_eqc_0_360.tif
gdal_translate -a_srs IAU_2015:30100 -a_ullr 0 90 360 -90 -co COMPRESS=DEFLATE moon_ldem4_eqc_0_360.tif moon_ldem4_geo_0_360.tif
gdalwarp -t_srs IAU_2015:30100 -te -180 -90 180 90 -ts 1440 720 -r bilinear --config CENTER_LONG 0 -co COMPRESS=DEFLATE moon_ldem4_geo_0_360.tif moon_ldem4_geo_pm180.tif
```

The golden manifests were first generated with the code of `e4e68352` (before the temp tile cache), and the temp tile cache (#36) gives the same output.
They were then regenerated with the NODATA fixes (#40, #41) and the depth-0 antimeridian fix (#39), which change the tiles at the NODATA borders and, with a geoid, the areas without data.
So this test requires #39, #40, #41, #42 (F21), #43 (F14) and #44 (F06) to be merged; F22-F25 require the planetary input fix (#33).
