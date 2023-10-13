package com.gaia3d.command;

import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

class MesherMainTest {

    @Test
    void main() throws FactoryException, TransformException, IOException
    {
        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output";
        String minTileDepth = String.valueOf(0);
        String maxTileDepth = String.valueOf(10);
        String refinementStrength = String.valueOf(1);
        String originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\data_geoTiff\\5m";

        convert(originalGeoTiffFolderPath, outputDirectory, minTileDepth, maxTileDepth, refinementStrength);
    }

    private void convert(String inputPath, String outputPath, String minTileDepth, String maxTileDepth, String refinementStrength) throws FactoryException, TransformException, IOException {
        String[] args = new String[]{
                "-inputFolderPath", inputPath,
                "-outputFolderPath", outputPath,
                "-minimumTileDepth", minTileDepth,
                "-maximumTileDepth", maxTileDepth,
                "-meshRefinementStrength", refinementStrength
        };
        MesherMain.main(args);
    }
}