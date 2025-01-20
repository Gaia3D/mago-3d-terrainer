package com.gaia3d.command;

import com.gaia3d.terrain.types.InterpolationType;
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
    private static final int DEFAULT_MAXIMUM_TILE_DEPTH = 14;
    private static final int DEFAULT_MOSAIC_SIZE = 32;
    private static final int DEFAULT_MAX_RASTER_SIZE = 8192;
    private static final double DEFAULT_INTENSITY = 4.0;

    private String version;
    private String javaVersionInfo;
    private String programInfo;
    private boolean layerJsonGenerate = false;
    private boolean debugMode = false;
    private boolean leaveTemp = false;

    private long startTime = 0;
    private long endTime = 0;

    private int mosaicSize;
    private int maxRasterSize;

    private String inputPath;
    private String outputPath;
    private String resizedTiffTempPath;
    private String splitTiffTempPath;
    private String tileTempPath;

    private String logPath;
    private int minimumTileDepth;
    private int maximumTileDepth;
    private InterpolationType interpolationType;
    private boolean calculateNormals;

    private double intensity;

    public static GlobalOptions getInstance() {
        if (instance.javaVersionInfo == null) {
            initVersionInfo();
        }
        return instance;
    }

    public static void init(CommandLine command) throws IOException {
        if (command.hasOption(CommandOptions.INPUT.getArgName())) {
            instance.setInputPath(command.getOptionValue(CommandOptions.INPUT.getArgName()));
            validateInputPath(new File(instance.getInputPath()).toPath());
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(CommandOptions.OUTPUT.getArgName())) {
            String outputPath = command.getOptionValue(CommandOptions.OUTPUT.getArgName());
            validateOutputPath(new File(outputPath).toPath());

            instance.setOutputPath(outputPath);
            instance.setResizedTiffTempPath(outputPath + File.separator + "ResizedTiffTemp");
            instance.setTileTempPath(outputPath + File.separator + "TileTemp");
            String inputPath = command.getOptionValue(CommandOptions.INPUT.getArgName());
            instance.setSplitTiffTempPath(inputPath + File.separator + "SplitTiffTemp");
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        if (command.hasOption(CommandOptions.LOG.getArgName())) {
            instance.setLogPath(command.getOptionValue(CommandOptions.LOG.getArgName()));
        }

        if (command.hasOption(CommandOptions.DEBUG.getArgName())) {
            instance.setDebugMode(true);
        }

        if (command.hasOption(CommandOptions.LEAVE_TEMP.getArgName())) {
            instance.setLeaveTemp(true);
        }

        if (command.hasOption(CommandOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
            int maxDepth = Integer.parseInt(command.getOptionValue(CommandOptions.MAXIMUM_TILE_DEPTH.getArgName()));
            if (maxDepth < 0) {
                log.warn("* Maximum tile depth is less than 0. Set to 0.");
                maxDepth = 0;
            } else if (maxDepth > 22) {
                log.warn("* Maximum tile depth is greater than 22. Set to 22.");
                maxDepth = 22;
            }
            instance.setMaximumTileDepth(Integer.parseInt(command.getOptionValue(CommandOptions.MAXIMUM_TILE_DEPTH.getArgName())));
        } else {
            instance.setMaximumTileDepth(DEFAULT_MAXIMUM_TILE_DEPTH);
        }

        if (command.hasOption(CommandOptions.MINIMUM_TILE_DEPTH.getArgName())) {
            int minDepth = Integer.parseInt(command.getOptionValue(CommandOptions.MINIMUM_TILE_DEPTH.getArgName()));
            if (minDepth < 0) {
                log.warn("* Minimum tile depth is less than 0. Set to 0.");
                minDepth = 0;
            } else if (minDepth > 22) {
                log.warn("* Minimum tile depth is greater than 22. Set to 22.");
                minDepth = 22;
            } else if (minDepth > instance.getMaximumTileDepth()) {
                log.warn("* Minimum tile depth is greater than maximum tile depth. Set to maximum tile depth.");
                minDepth = 0;
            }
            instance.setMinimumTileDepth(minDepth);
        } else {
            instance.setMinimumTileDepth(DEFAULT_MINIMUM_TILE_DEPTH);
        }

        if (command.hasOption(CommandOptions.JSON.getArgName())) {
            instance.setLayerJsonGenerate(true);
        }

        if (instance.getMinimumTileDepth() > instance.getMaximumTileDepth()) {
            throw new IllegalArgumentException("Minimum tile depth must be less than or equal to maximum tile depth.");
        }

        if (command.hasOption(CommandOptions.INTERPOLATION_TYPE.getArgName())) {
            String interpolationType = command.getOptionValue(CommandOptions.INTERPOLATION_TYPE.getArgName());
            InterpolationType type;
            try {
                type =  InterpolationType.fromString(interpolationType);
            } catch (IllegalArgumentException e) {
                log.warn("* Interpolation type is not valid. Set to bilinear.");
                type = DEFAULT_INTERPOLATION_TYPE;
            }
            instance.setInterpolationType(type);
        } else {
            instance.setInterpolationType(DEFAULT_INTERPOLATION_TYPE);
        }

        if (command.hasOption(CommandOptions.TILING_MOSAIC_SIZE.getArgName())) {
            instance.setMosaicSize(Integer.parseInt(command.getOptionValue(CommandOptions.TILING_MOSAIC_SIZE.getArgName())));
        } else {
            instance.setMosaicSize(DEFAULT_MOSAIC_SIZE);
        }

        if (command.hasOption(CommandOptions.RASTER_MAXIMUM_SIZE.getArgName())) {
            instance.setMaxRasterSize(Integer.parseInt(command.getOptionValue(CommandOptions.RASTER_MAXIMUM_SIZE.getArgName())));
        } else {
            instance.setMaxRasterSize(DEFAULT_MAX_RASTER_SIZE);
        }

        if (command.hasOption(CommandOptions.INTENSITY.getArgName())) {
            double intensity = Double.parseDouble(command.getOptionValue(CommandOptions.INTENSITY.getArgName()));
            if (intensity < 1) {
                log.warn("* Intensity value is less than 1. Set to 1.");
                intensity = 1;
            } else if (intensity > 16) {
                log.warn("* Intensity value is greater than 16. Set to 16.");
                intensity = 16;
            }
            instance.setIntensity(intensity);
        } else {
            instance.setIntensity(DEFAULT_INTENSITY);
        }

        instance.setCalculateNormals(command.hasOption(CommandOptions.CALCULATE_NORMALS.getArgName()));
        printGlobalOptions();
    }

    protected static void printGlobalOptions() {
        log.info("Input Path: {}", instance.getInputPath());
        log.info("Output Path: {}", instance.getOutputPath());
        log.info("Log Path: {}", instance.getLogPath());
        log.info("Minimum Tile Depth: {}", instance.getMinimumTileDepth());
        log.info("Maximum Tile Depth: {}", instance.getMaximumTileDepth());
        log.info("Intensity: {}", instance.getIntensity());
        log.info("Interpolation Type: {}", instance.getInterpolationType());
        log.info("Calculate Normals: {}", instance.isCalculateNormals());
        log.info("Tiling Mosaic Size: {}", instance.getMosaicSize());
        log.info("Tiling Max Raster Size: {}", instance.getMaxRasterSize());
        log.info("Layer Json Generate: {}", instance.isLayerJsonGenerate());
        log.info("Debug Mode: {}", instance.isDebugMode());
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
        title = title == null ? "3d-terrainer" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;

        instance.setStartTime(System.currentTimeMillis());
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }
}
