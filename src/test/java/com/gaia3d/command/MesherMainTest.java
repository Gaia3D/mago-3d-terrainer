package com.gaia3d.command;

import com.gaia3d.wgs84Tiles.TileWgs84Utils;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

class MesherMainTest {
    //****************************************************************
    // Note : the outFolder must be different from the inputFolder.***
    //****************************************************************

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoMesherMain.main(args);
    }

    @Test
    void sample() {
        String pathName = "sample";
        String[] args = new String[]{
                "-input", "D:\\dem\\sample-input\\",
                "-output", "D:\\dem\\sample-output\\" + pathName,
                "-min", "0",
                "-max", "15",
                //"-d",
        };
        MagoMesherMain.main(args);
    }


    @Test
    void main() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String originalGeoTiffFolderPath = "D:\\dem\\input";
        String outputDirectory = "D:\\dem\\korea-3m\\";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void main_korea() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\data_geoTiff\\5m";
        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output\\";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void main_earth() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String originalGeoTiffFolderPath = "D:\\data\\DEM\\wholeEarth";
        String outputDirectory = "D:\\data\\DEM_output\\output\\";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(6);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void main2() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String originalGeoTiffFolderPath = "D:\\dem\\input";
        String outputDirectory = "D:\\dem\\output2\\";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(18);
        String refinementStrength = String.valueOf(1);
        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    //@Test
    void main_smallMountainForTrees() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\smallDatas";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    //@Test
    void main_getElevationTest() throws FactoryException, TransformException, IOException {
        String outputPath = "D:\\QuantizedMesh_JavaProjects\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\smallDatas";

        String[] args = new String[]{"-inputFolderPath", originalGeoTiffFolderPath, "-outputFolderPath", outputPath, "-minimumTileDepth", minTileDepth, "-maximumTileDepth", maxTileDepth, "-meshRefinementStrength", refinementStrength};
        //MagoMesherMain.mainTest(args);
    }

    @Test
    void main_ws2() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String outputDirectory = "D:\\data\\DEM_output\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(17);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\data\\DEM";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    @Test
    void main_sejong() throws FactoryException, TransformException, IOException {
        //*******************************************************************
        // Note : the outputFolder must be different from the inputFolder.***
        //*******************************************************************
        String outputDirectory = "D:\\data\\DEM_output\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\data\\DEM\\sejongDEM";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    //@Test
    void main_generalTest() throws FactoryException, TransformException, IOException {
        //****************************************************************
        // Note : the outFolder must be different from the inputFolder.***
        //****************************************************************
        String outputDirectory = "D:\\data\\DEM_output\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(16);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\data\\data_geoTiff\\5m";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    //@Test
    void main_generalTest2() throws FactoryException, TransformException, IOException {
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(14);
        int hola = 0;

    }

    private void convert(String inputPath, String outputPath, String minTileDepth, String maxTileDepth, String refinementStrength) throws FactoryException, TransformException, IOException {
        String[] args = new String[]{"-i", inputPath, "-o", outputPath, "-mn", minTileDepth, "-mx", maxTileDepth, "-rs", refinementStrength, "-d"};
        MagoMesherMain.main(args);
    }
}