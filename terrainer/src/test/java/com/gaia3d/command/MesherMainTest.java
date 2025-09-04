package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

@Deprecated
@Slf4j
class MesherMainTest {
    //****************************************************************
    // Note : the outFolder must be different from the inputFolder
    //****************************************************************

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleDirect() {
        String pathName = "sample";
        String[] args = new String[]{
                "-input", "D:/dem/sample-input/",
                "-output", "D:/dem/sample-output/" + pathName,
                "-log", "D:/dem/sample-output/" + pathName + "/log.txt",
                "-min", "0",
                "-max", "10",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample() {
        String pathName = "sample";
        String[] args = new String[]{
                "-input", "D:/dem/sample-input/",
                "-output", "D:/dem/sample-output/" + pathName,
                "-log", "D:/dem/sample-output/" + pathName + "/log.txt",
                "-min", "0",
                "-max", "10",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void merge() {
        String pathName = "merge";
        String[] args = new String[]{
                "-input", "D:/dem/merge-input/",
                "-output", "D:/dem/merge-output/",
                "-log", "D:/dem/merge-output/log.txt",
                "-min", "0",
                "-max", "12",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void globalSample() {
        String pathName = "sample-input-global";
        String[] args = new String[]{
                "-input", "D:/dem/sample-input-global/",
                "-output", "D:/dem/sample-output-global/",
                "-log", "D:/dem/sample-output/" + pathName + "/log.txt",
                "-min", "0",
                "-max", "8",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void main() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder
        //****************************************************************
        String originalGeoTiffFolderPath = "D:/dem/input";
        String outputDirectory = "D:/dem/korea-3m/";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainKorea() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder
        //****************************************************************
        String originalGeoTiffFolderPath = "D:/QuantizedMesh_JavaProjects/data_geoTiff/5m";
        String outputDirectory = "D:/QuantizedMesh_JavaProjects/output/";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(12);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainKoreaOneGeotiff() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\data_geoTiff_1file_korea";
        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output\\";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(15);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainEarth() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder
        //****************************************************************
        String originalGeoTiffFolderPath = "D:/data/DEM/wholeEarth";
        String outputDirectory = "D:/data/DEM_output/output/";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(6);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainAsia() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(9);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\data\\(20240627)Asia-Geotiff";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }


    @Test
    void mainWs2() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder
        //****************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/DEM";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainSeoul20240912() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/TerrainData_issues";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainBangKok20240919() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/TerrainData_issues/Thailand_yeonhwa";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void busanLittle_20250115() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************

        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(15);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/busanLittle05_4326";
        String outputDirectory = "D:/data/mago-server/output/result_busanLittle05_4326";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void allKoreaSouthJinHun_20250115() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************

        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(14);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/allKoreaSouthJinHun_20250115";
        String outputDirectory = "D:/data/mago-server/output/result_allKoreaSouthJinHun_20250402";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void jiRiWon_20250224() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************

        // 2 levels of geoTiff files. The 1rst is 1m of definition and the 2nd is 5m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(14);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/(20250224) 지리원 DEM";
        String outputDirectory = "D:/data/mago-server/output/result_jiRiWon_20250224";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void busan_20250310() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // Conversion time:
        // maxTileDepth = xx takes x hour and xx minutes.

        // 2 levels of geoTiff files. The 1rst is 1 m of definition, and the 2nd is 5 m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(18);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/busan_20250310";
        String outputDirectory = "D:/data/mago-server/output/result_smallBusan_20250319_L18";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void smallKyongSangDo_20250331() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // 2 levels of geoTiff files. The 1rst is 1m of definition, and the 2nd is 5m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(14);
        String refinementStrength = String.valueOf(3);
        String originalGeoTiffFolderPath = "D:/data/DEM/kyongSando_small_5186";
        String outputDirectory = "D:/data/mago-server/output/result_kyongSando_small_5186";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void sejong_20250418() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // 2 levels of geoTiff files. The 1rst is 1m of definition, and the 2nd is 5m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(3);
        String originalGeoTiffFolderPath = "D:/data/DEM/Sejong20240704";
        String outputDirectory = "D:/data/mago-server/output/result_Sejong20240704";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void smallBusan_oneTiffTest() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // 2 levels of geoTiff files. The 1rst is 1m of definition, and the 2nd is 5m of definition.

        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(15);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/smallBusanTestOneTiff";
        String outputDirectory = "D:/data/mago-server/output/terrain_smallBusanTestOneTiff_L15";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void multi_resolution_noData() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // Conversion time:
        // maxTileDepth = xx takes x hour and xx minutes.

        // 2 levels of geoTiff files. The 1rst is 1 m of definition, and the 2nd is 5 m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/multi-resolution-nodata";
        String outputDirectory = "D:/data/mago-server/output/multi-resolution-nodata_L16";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void multi_resolution_big() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note: the outputFolder must be different from the inputFolder
        //*******************************************************************

        // Conversion time:
        // maxTileDepth = 14 takes 1 hour and 56 minutes.

        // 2 levels of geoTiff files. The 1rst is 1 m of definition, and the 2nd is 5 m of definition.
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(14);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/multi-resolution-big";
        String outputDirectory = "D:/data/mago-server/output/multi-resolution-big_L14";
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }


    private void convert(String inputPath, String outputPath, String minTileDepth, String maxTileDepth, String refinementStrength) throws FactoryException, TransformException, IOException {
        String logPath = outputPath + "/log.txt";

        String[] args = new String[]{"-i", inputPath, "-o", outputPath, "-log", logPath, "-min", minTileDepth, "-max", maxTileDepth, "-is", refinementStrength, "-cn", "-debug"};
        //String[] args = new String[]{"-i", inputPath, "-o", outputPath, "-log", logPath, "-min", minTileDepth, "-max", maxTileDepth, "-is", refinementStrength, "-cn", "-nv", "0"};
        MagoTerrainerMain.main(args);
    }
}