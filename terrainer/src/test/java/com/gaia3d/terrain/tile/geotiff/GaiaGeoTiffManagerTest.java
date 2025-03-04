package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j
class GaiaGeoTiffManagerTest {

    @Test
    void reprojection() {
        Configurator.initConsoleLogger();

        File inputFile = new File("D:\\dem30-5186\\dem30-5186-part.tif"); // 5186
        //File inputFile = new File("D:\\dem05-5186-part-real\\dem05-parts-5186-crop.tif"); // 5186
        //File inputFile = new File("D:\\dem05-5186\\dem05.tif"); // 5186

        File outputFile = new File("D:\\temp", inputFile.getName());
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        /*try {
            targetCRS = CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }*/

        RasterStandardizer reprojector = new RasterStandardizer();
        GridCoverage2D source = reprojector.readGeoTiff(inputFile);
        //GridCoverage2D target = reprojector.wrap(source, targetCRS);
        //GridCoverage2D target = reprojector.affine(source, targetCRS);
        //GridCoverage2D target = reprojector.reproject(source, targetCRS);
        //GridCoverage2D target = reprojector.reproject2(source, targetCRS);

        List<GridCoverage2D> reprojectedTiles = null;
        /*try {
            List<GridCoverage2D> tiles = reprojector.splitCoverageIntoTiles(inputFile, source, 1024);

            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            reprojectedTiles = pool.submit(() -> tiles.parallelStream().map(tile -> reprojector.reprojectTile(tile, targetCRS)).collect(Collectors.toList())).get();
        } catch (TransformException | IOException | InterruptedException | ExecutionException e) {
            log.error("Failed to reproject tiles", e);
            throw new RuntimeException(e);
        }*/

        reprojectedTiles.forEach(tile -> {
            File tileFile = new File(outputFile.getParent(), UUID.randomUUID() + ".tif");
            reprojector.writeGeotiff(tile, tileFile);
        });
    }
}