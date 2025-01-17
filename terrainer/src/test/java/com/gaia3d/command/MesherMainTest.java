package com.gaia3d.command;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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
    void mainSejongJinho20240704() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\data\\DEM\\Sejong20240704";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }


    //@Test
    void mainSmallMountainForTrees() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder
        //****************************************************************
        String outputDirectory = "D:/QuantizedMesh_JavaProjects/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/QuantizedMesh_JavaProjects/smallDatas";

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
    void mainSejong() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/DEM/sejongDEM";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void mainSejongCompressedGeoTiff() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/DEM/sejongCompressedGeoTiff";

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
    void mainBuildingPinchosMini() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/DEM_output/Terrain_Contour_pinchos";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(18);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:/data/TerrainData_issues/Jinho_20240923/terrainContour_pinchos";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void miniSeoulYeonHwa_20250108() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************
        String outputDirectory = "D:/data/mago-server/output/SeoulSmallYeonHwa_20250108";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(14);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/TerrainData_issues/SeoulSmallYeonHwa_20250108";

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
        String maxTileDepth = String.valueOf(12);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/allKoreaSouthJinHun_20250115";
        String outputDirectory = "D:/data/mago-server/output/result_allKoreaSouthJinHun_20250115";

//        String originalGeoTiffFolderPath = "D:/data/DEM/busanLittle05_5186";
//        String outputDirectory = "D:/data/mago-server/output/result_busanLittle05_5186";

//        String originalGeoTiffFolderPath = "D:/data/DEM/yeosu-4326";
//        String outputDirectory = "D:/data/mago-server/output/result_yeosu-4326";

//        String originalGeoTiffFolderPath = "D:/data/DEM/20250116-dem05-4326/dem05-wgs84";
//        String outputDirectory = "D:\\data\\DEM\\20250116-dem05-4326\\dem05-wgs84-crop";

//        String originalGeoTiffFolderPath = "D:/data/DEM/seoul2geoTiffs";
//        String outputDirectory = "D:/data/mago-server/output/result_seoul2geoTiffs";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void korea_westSouth() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder
        //*******************************************************************

        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(12);
        String refinementStrength = String.valueOf(4);
        String originalGeoTiffFolderPath = "D:/data/DEM/korea_westSouth_5186";
        String outputDirectory = "D:/data/mago-server/output/result_korea_westSouth_5186";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }
    
    private void convert(String inputPath, String outputPath, String minTileDepth, String maxTileDepth, String refinementStrength) throws FactoryException, TransformException, IOException {
        String[] args = new String[]{"-i", inputPath, "-o", outputPath, "-min", minTileDepth, "-max", maxTileDepth, "-is", refinementStrength, "-cn"};
        MagoTerrainerMain.main(args);
    }
}