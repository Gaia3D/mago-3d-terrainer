package com.gaia3d.command;

import com.gaia3d.process.ProcessOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileExistsException;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {
    /* singleton */
    private static final GlobalOptions instance = new GlobalOptions();

    private final String DEFAULT_INTERPOLATION_TYPE = "bilinear";
    private final int DEFAULT_MINIMUM_TILE_DEPTH = 0;
    private final int DEFAULT_MAXIMUM_TILE_DEPTH = 14;

    private String inputPath;
    private String outputPath;
    private String resizedTiffTempPath;
    private String tileTempPath;

    private String logPath;
    private int minimumTileDepth;
    private int maximumTileDepth;
    private String interpolationType;
    private boolean calculateNormals;

    public static void init(CommandLine command) throws IOException {
        if (command.hasOption(ProcessOptions.INPUT.getArgName())) {
            instance.setInputPath(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
            validateInputPath(new File(instance.getInputPath()).toPath());
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
            String outputPath = command.getOptionValue(ProcessOptions.OUTPUT.getArgName());
            validateOutputPath(new File(outputPath).toPath());

            instance.setOutputPath(outputPath);
            instance.setResizedTiffTempPath(outputPath + File.separator + "ResizedTiffTemp");
            instance.setTileTempPath(outputPath + File.separator + "TileTemp");
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        if (command.hasOption(ProcessOptions.LOG.getArgName())) {
            instance.setLogPath(command.getOptionValue(ProcessOptions.LOG.getArgName()));
        }

        if (command.hasOption(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())) {
            instance.setMinimumTileDepth(Integer.parseInt(command.getOptionValue(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())));
        } else {
            instance.setMinimumTileDepth(instance.DEFAULT_MINIMUM_TILE_DEPTH);
        }

        if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
            instance.setMaximumTileDepth(Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())));
        } else {
            instance.setMaximumTileDepth(instance.DEFAULT_MAXIMUM_TILE_DEPTH);
        }

        if (command.hasOption(ProcessOptions.INTERPOLATION_TYPE.getArgName())) {
            instance.setInterpolationType(command.getOptionValue(ProcessOptions.INTERPOLATION_TYPE.getArgName()));
        } else {
            instance.setInterpolationType(instance.DEFAULT_INTERPOLATION_TYPE);
        }

        instance.setCalculateNormals(command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName()));

        printGlobalOptions();
    }

    protected static void printGlobalOptions() {
        log.info("----------------------------------------");
        log.info("Input Path: {}", instance.getInputPath());
        log.info("Output Path: {}", instance.getOutputPath());
        log.info("Log Path: {}", instance.getLogPath());
        log.info("Minimum Tile Depth: {}", instance.getMinimumTileDepth());
        log.info("Maximum Tile Depth: {}", instance.getMaximumTileDepth());
        log.info("Interpolation Type: {}", instance.getInterpolationType());
        log.info("Calculate Normals: {}", instance.isCalculateNormals());
        log.info("----------------------------------------");
    }

    protected static void validateInputPath(Path path) throws IOException {
        File output = path.toFile();
        if (!output.exists()) {
            throw new FileExistsException(String.format("%s Path is not exist.", path));
        } else if (!output.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
    }

    protected static void validateOutputPath(Path path) throws IOException {
        File output = path.toFile();
        if (!output.exists()) {
            boolean isSuccess = output.mkdirs();
            if (!isSuccess) {
                throw new FileExistsException(String.format("%s Path is not exist.", path));
            } else {
                log.info("Created new output directory: {}", path);
            }
        } else if (!output.isDirectory()) {
            throw new NotDirectoryException(String.format("%s Path is not directory.", path));
        } else if (!output.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
    }
}
