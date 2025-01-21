package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.terrain.tile.GaiaThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.*;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.processing.operation.Affine;
import org.geotools.coverage.processing.operation.Warp;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.Interpolation;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class GeoTiffReprojector {

    public void reprojectMain(GridCoverage2D source, File inputFile, File outputPath, CoordinateReferenceSystem targetCRS) {
        List<GridCoverage2D> reprojectedTiles = null;
        try {
            List<GridCoverage2D> tiles = splitCoverageIntoTiles(inputFile, source, 1024);

            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            reprojectedTiles = pool.submit(() -> tiles.parallelStream()
                    .map(tile -> reprojectTile(tile, targetCRS))
                    .collect(Collectors.toList())
            ).get();
        } catch (TransformException | IOException | InterruptedException | ExecutionException e) {
            log.error("Failed to reproject tiles", e);
            throw new RuntimeException(e);
        }

        int total = reprojectedTiles.size();
        AtomicInteger count = new AtomicInteger(0);
        GaiaThreadPool gaiaThreadPool = new GaiaThreadPool();
        List<Runnable> tasks = reprojectedTiles.stream()
                .map(tile -> (Runnable) () -> {
                    File tileFile = new File(outputPath, UUID.randomUUID() + ".tif");
                    log.info("[Reprojected Tile][{}/{}] : {}", count.incrementAndGet(), total, tileFile.getAbsolutePath());
                    writeGeoTiff(tile, tileFile);
                }).collect(Collectors.toList());
        try {
            gaiaThreadPool.execute(tasks);
        } catch (InterruptedException e) {
            log.error("Failed to execute tasks", e);
            throw new RuntimeException(e);
        }

        /*reprojectedTiles.forEach(tile -> {
            File tileFile = new File(outputPath, UUID.randomUUID().toString() + ".tif");
            log.info("[Reprojected Tile] : {}", tileFile.getAbsolutePath());
            writeGeoTiff(tile, tileFile);
        });*/
    }

    public GridCoverage2D reproject2(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS) {
        try {
            // 원본 좌표체계 가져오기
            CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem2D();

            // 좌표 변환 객체 생성
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

            // 재투영 수행
            CoverageProcessor processor = CoverageProcessor.getInstance();
            Operation operation = processor.getOperation("Resample");

            ParameterValueGroup params = operation.getParameters();
            params.parameter("Source").setValue(coverage);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            params.parameter("GridGeometry").setValue(null); // 기본 그리드 사용
            params.parameter("InterpolationType").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST));

            return (GridCoverage2D) processor.doOperation(params);
        } catch (Exception e) {
            log.error("Failed to reproject coverage :", e);
            throw new RuntimeException(e);
        }
    }

    public GridCoverage2D reproject(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS) {
        Envelope envelope = coverage.getEnvelope();
        ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(envelope);
        ReferencedEnvelope targetEnvelope = null;
        try {
            targetEnvelope = sourceEnvelope.transform(targetCRS, false);
        } catch (TransformException | FactoryException e) {
            log.error("Failed to transform envelope :", e);
            throw new RuntimeException(e);
        }

        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D target = factory.create("reprojected", coverage.getRenderedImage(), targetEnvelope);
        return target;
    }

    public GridCoverage2D reprojectOriginal(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS) {
        try {
            CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem();
            if (sourceCRS.equals(targetCRS)) {
                return coverage;
            }
            Operations ops = new Operations(null);
            GridCoverage2D reprojectedCoverage = (GridCoverage2D) ops.resample(coverage, targetCRS);
            //GridCoverage2D reprojectedCoverage = (GridCoverage2D) ops.resample(coverage, coverage.getGridGeometry().getEnvelope(), Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            //GridCoverage2D reprojectedCoverage = (GridCoverage2D) ops.resample(coverage, targetCRS, coverage.getGridGeometry(), Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            return reprojectedCoverage;
        } catch (Exception e) {
            log.error("Failed to reproject GeoTiff file :", e);
            return coverage;
            //throw new RuntimeException(e);
        }
    }

    public GridCoverage2D affine(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS) {
        try {
            Operations ops = new Operations(null);
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            CoordinateReferenceSystem sourceCRS = gridGeometry.getCoordinateReferenceSystem();

            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
            MathTransform2D transform2D = (MathTransform2D) transform;

            Affine affine = new Affine();
            AffineTransform affineTransform = new AffineTransform();
            AffineTransform2D affineTransform2D = new AffineTransform2D(affineTransform);

            // 5. 좌표 변환 수행 및 리샘플링
            GridCoverage2D reprojectedCoverage = (GridCoverage2D) ops.affine(coverage, affineTransform, null, null);
            return reprojectedCoverage;
        } catch (Exception e) {
            log.error("Failed to reproject GeoTiff file :", e);
            throw new RuntimeException(e);
        }
    }

    public GridCoverage2D wrap(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS) {
        try {
            Operations ops = new Operations(null);
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            CoordinateReferenceSystem sourceCRS = gridGeometry.getCoordinateReferenceSystem();

            Warp warp = new Warp();
            ParameterValueGroup params = warp.getParameters();
            params.parameter("Source").setValue(coverage);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);


            /*Warp warp = null;
            try {
                MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
                MathTransform2D transform2D = (MathTransform2D) transform;

                WarpBuilder warpBuilder = new WarpBuilder(0.0);

                Rectangle rectangle = new Rectangle(0, 0, coverage.getRenderedImage().getWidth(), coverage.getRenderedImage().getHeight());
                warp = warpBuilder.buildWarp(transform2D, rectangle);
                //warp = WarpTransform2D.getWarp(coverage.getName(), transform2D);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }*/
            GridCoverage2D reprojectedCoverage = (GridCoverage2D) warp.doOperation(params, null);

            // 5. 좌표 변환 수행 및 리샘플링
            //GridCoverage2D reprojectedCoverage = (GridCoverage2D) ops.warp(coverage, warp);
            return reprojectedCoverage;
        } catch (Exception e) {
            log.error("Failed to reproject GeoTiff file :", e);
            throw new RuntimeException(e);
        }
    }

    public GridCoverage2D readGeoTiff(File file) {
        try {
            GeoTiffReader reader = new GeoTiffReader(file);
            return reader.read(null);
        } catch (Exception e) {
            log.error("Failed to read GeoTiff file. ", e);
            throw new RuntimeException(e);
        }
    }

    public void writeGeoTiff(GridCoverage2D coverage, File outputFile) {
        try {
            GeoTiffWriter writer = new GeoTiffWriter(outputFile);
            writer.write(coverage, null);
            writer.dispose();
        } catch (Exception e) {
            log.error("Failed to write GeoTiff file. ", e);
        }
    }


    // 타일 분할 메서드
    public List<GridCoverage2D> splitCoverageIntoTiles(File inputFile, GridCoverage2D coverage, int tileSize) throws TransformException, IOException {

        AbstractGridFormat format = GridFormatFinder.findFormat(inputFile);
        GridCoverageReader reader = format.getReader(inputFile);

        List<GridCoverage2D> tiles = new ArrayList<>();

        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        GridEnvelope gridRange = gridGeometry.getGridRange();
        int width = gridRange.getSpan(0);
        int height = gridRange.getSpan(1);

        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                int xMax = Math.min(x + tileSize, width);
                int yMax = Math.min(y + tileSize, height);

                // 해당 영역의 타일 범위 추출
                ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(
                        gridGeometry.gridToWorld(new GridEnvelope2D(x, y, xMax - x, yMax - y)),
                        coverage.getCoordinateReferenceSystem()
                );

                //ParameterValueGroup params = reader.getFormat().getReadParameters();
                //params.parameter("Envelope").setValue(tileEnvelope);
                //GridCoverage2D tile = (GridCoverage2D) reader.read(params);

                GridCoverage2D tile = cropCoverage(coverage, tileEnvelope);
                tiles.add(tile);
            }
        }
        return tiles;
    }

    public GridCoverage2D cropCoverage(GridCoverage2D coverage, ReferencedEnvelope envelope) {
        try {
            CoverageProcessor processor = CoverageProcessor.getInstance();
            Operations ops = Operations.DEFAULT;
            return (GridCoverage2D) ops.crop(coverage, envelope);
        } catch (Exception e) {
            throw new RuntimeException("Failed to crop coverage", e);
        }
    }

    // 타일 재투영 메서드
    public GridCoverage2D reprojectTile(GridCoverage2D tile, CoordinateReferenceSystem targetCRS) {
        try {
            CoordinateReferenceSystem sourceCRS = tile.getCoordinateReferenceSystem();
            if (sourceCRS.equals(targetCRS)) {
                return tile;
            }

            CoverageProcessor processor = CoverageProcessor.getInstance();
            Operation operation = processor.getOperation("Resample");

            ParameterValueGroup params = operation.getParameters();
            params.parameter("Source").setValue(tile);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            params.parameter("InterpolationType").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST));

            return (GridCoverage2D) processor.doOperation(params);
        } catch (Exception e) {
            log.error("Failed to reproject tile", e);
            return tile;
            //throw new RuntimeException("Failed to reproject tile", e);
        }
    }

    // 타일 병합 메서드 (GeoTools에서는 직접 지원이 어려우므로 GDAL 등 추가 도구 활용 권장)
    public GridCoverage2D mergeTiles(List<GridCoverage2D> tiles) {
        // 간단한 병합 로직 (고해상도 데이터에서는 최적화 필요)
        // 실제 구현에서는 외부 라이브러리(GDAL 등) 활용을 추천
        throw new UnsupportedOperationException("Tile merging is not yet implemented.");
    }

    // 결과 저장 메서드
    public void saveCoverageToFile(GridCoverage2D coverage, File outputFile) {
        try {
            GeoTiffWriter writer = new GeoTiffWriter(outputFile);
            writer.write(coverage, null);
            writer.dispose();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save coverage to file", e);
        }
    }
}
