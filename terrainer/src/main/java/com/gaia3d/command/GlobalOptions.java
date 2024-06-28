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
    private static final InterpolationType DEFAULT_INTERPOLATION_TYPE = InterpolationType.BILINEAR;
    private static final int DEFAULT_MINIMUM_TILE_DEPTH = 0;
    private static final int DEFAULT_MAXIMUM_TILE_DEPTH = 12;
    private static final int DEFAULT_MOSAIC_SIZE = 64;
    private static final boolean DEFAULT_CALCULATE_NORMALS = false;

    private String version;
    private String javaVersionInfo;
    private String programInfo;

    private long startTime = 0;
    private long endTime = 0;

    private int mosaicSize;

    private String inputPath;
    private String outputPath;
    private String resizedTiffTempPath;
    private String tileTempPath;

    private String logPath;
    private int minimumTileDepth;
    private int maximumTileDepth;
    private InterpolationType interpolationType;
    private boolean calculateNormals;

    public static GlobalOptions getInstance() {
        if (instance.javaVersionInfo == null) {
            initVersionInfo();
        }
        return instance;
    }

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
            instance.setMinimumTileDepth(DEFAULT_MINIMUM_TILE_DEPTH);
        }

        if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
            instance.setMaximumTileDepth(Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())));
        } else {
            instance.setMaximumTileDepth(DEFAULT_MAXIMUM_TILE_DEPTH);
        }

        if (command.hasOption(ProcessOptions.INTERPOLATION_TYPE.getArgName())) {
            String interpolationType = command.getOptionValue(ProcessOptions.INTERPOLATION_TYPE.getArgName());
            InterpolationType type = InterpolationType.fromString(interpolationType);
            instance.setInterpolationType(type);
        } else {
            instance.setInterpolationType(DEFAULT_INTERPOLATION_TYPE);
        }

        if (command.hasOption(ProcessOptions.TILING_MOSAIC_SIZE.getArgName())) {
            instance.setMosaicSize(Integer.parseInt(command.getOptionValue(ProcessOptions.TILING_MOSAIC_SIZE.getArgName())));
        } else {
            instance.setMosaicSize(DEFAULT_MOSAIC_SIZE);
        }

        instance.setCalculateNormals(command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName()));

        printGlobalOptions();
    }

    protected static void printGlobalOptions() {
        log.info("Input Path: {}", instance.getInputPath());
        log.info("Output Path: {}", instance.getOutputPath());
        log.info("Log Path: {}", instance.getLogPath());
        log.info("Minimum Tile Depth: {}", instance.getMinimumTileDepth());
        log.info("Maximum Tile Depth: {}", instance.getMaximumTileDepth());
        log.info("Interpolation Type: {}", instance.getInterpolationType());
        log.info("Calculate Normals: {}", instance.isCalculateNormals());
        log.info("Tiling Mosaic Size: {}", instance.getMosaicSize());
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

    private static void initVersionInfo() {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersionInfo = "JAVA Version : " + javaVersion + " (" + javaVendor + ") ";
        String version = MagoTerrainerMain.class.getPackage().getImplementationVersion();
        String title = MagoTerrainerMain.class.getPackage().getImplementationTitle();
        String vendor = MagoTerrainerMain.class.getPackage().getImplementationVendor();
        version = version == null ? "dev-version" : version;
        title = title == null ? "3d-mesher" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;

        instance.setStartTime(System.currentTimeMillis());
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }
}
