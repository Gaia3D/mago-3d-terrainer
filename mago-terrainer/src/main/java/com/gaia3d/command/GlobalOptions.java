package com.gaia3d.command;

import com.gaia3d.basic.exception.Reporter;
import com.gaia3d.terrain.types.InterpolationType;
import com.gaia3d.terrain.types.PriorityType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileExistsException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@Slf4j
public class GlobalOptions {
    /* singleton */
    private static GlobalOptions instance = new GlobalOptions();
    private Reporter reporter;

    /* Constants */
    private static final InterpolationType DEFAULT_INTERPOLATION_TYPE = InterpolationType.BILINEAR;
    private static final int DEFAULT_MINIMUM_TILE_DEPTH = 0;
    private static final int DEFAULT_MAXIMUM_TILE_DEPTH = 14;
    private static final int DEFAULT_MOSAIC_SIZE = 16;
    private static final int DEFAULT_MAX_RASTER_SIZE = 4000;
    private static final double DEFAULT_INTENSITY = 4.0;
    private static final double DEFAULT_NO_DATA_VALUE = -9999.0;
    private static final CoordinateReferenceSystem DEFAULT_TARGET_CRS = DefaultGeographicCRS.WGS84;
    private static final TilingSchema DEFAULT_TILING_SCHEMA = TilingSchema.GEODETIC;
    private static final String DEFAULT_TEMP_DIR = "temp";

    /* Program Information */
    private String version;
    private String javaVersionInfo;
    private String programInfo;
    private long startTime = 0;
    private long endTime = 0;

    /* Default Options */
    private String inputPath;
    private String outputPath;
    private String geoidPath;
    private String logPath;
    private boolean layerJsonGenerate = false;
    private boolean debugMode = false;
    private boolean leaveTemp = false;
    private boolean isContinue = false;

    /* Tiling options */
    private int minimumTileDepth;
    private int maximumTileDepth;
    private InterpolationType interpolationType;
    private PriorityType priorityType;
    private double noDataValue;
    private double intensity;

    /* Extensions */
    private boolean isCalculateNormalsExtension;
    private boolean isMetaDataExtension;
    private boolean isWaterMaskExtension;

    /* Migration options */
    private int mosaicSize;
    private int maxRasterSize;

    /* Temporary paths for processing */
    private String geoidTempPath;
    private String standardizeTempPath;
    private String resizedTiffTempPath;
    private String splitTiffTempPath;
    private String tileTempPath;

    private CoordinateReferenceSystem inputCRS = null;
    private CoordinateReferenceSystem outputCRS;
    private TilingSchema tilingSchema;

    private GlobalOptions() {};

    public static GlobalOptions getInstance() {
        if (instance.javaVersionInfo == null) {
            initVersionInfo();
            instance.reporter = new Reporter("mago-3d-terrainer", instance.getVersion());
        }
        return instance;
    }

    public static void recreateInstance() {
        log.info("[INFO] Recreating GlobalOptions instance.");
        GlobalOptions.instance = new GlobalOptions();
    }

    public static void init(CommandLine command) throws IOException {
        if (command.hasOption(CommandOptions.INPUT.getLongName())) {
            instance.setInputPath(command.getOptionValue(CommandOptions.INPUT.getLongName()));
            validateInputPath(new File(instance.getInputPath()).toPath());
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(CommandOptions.OUTPUT.getLongName())) {
            String outputPath = command.getOptionValue(CommandOptions.OUTPUT.getLongName());
            validateOutputPath(new File(outputPath).toPath());
            instance.setOutputPath(outputPath);
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        if (command.hasOption(CommandOptions.TEMP_PATH.getLongName())) {
            String tempPath = command.getOptionValue(CommandOptions.TEMP_PATH.getLongName());
            File tempDir = new File(tempPath);
            File resizedDir = new File(tempDir, "resized");
            File splitDir = new File(tempDir, "split");
            File standardizeDir = new File(tempDir, "standardization");
            File geoidDir = new File(tempDir, "geoid");
            instance.setTileTempPath(tempDir.getAbsolutePath());
            instance.setResizedTiffTempPath(resizedDir.getAbsolutePath());
            instance.setSplitTiffTempPath(splitDir.getAbsolutePath());
            instance.setStandardizeTempPath(standardizeDir.getAbsolutePath());
            instance.setGeoidTempPath(geoidDir.getAbsolutePath());
        } else {
            File tempDir = new File(instance.getOutputPath(), DEFAULT_TEMP_DIR);
            File resizedDir = new File(tempDir, "resized");
            File splitDir = new File(tempDir, "split");
            File standardizeDir = new File(tempDir, "standardization");
            instance.setTileTempPath(tempDir.getAbsolutePath());
            instance.setResizedTiffTempPath(resizedDir.getAbsolutePath());
            instance.setSplitTiffTempPath(splitDir.getAbsolutePath());
            instance.setStandardizeTempPath(standardizeDir.getAbsolutePath());
            instance.setGeoidTempPath(new File(tempDir, "geoid").getAbsolutePath());
        }

        if (command.hasOption(CommandOptions.GEOID_PATH.getLongName())) {
            instance.setGeoidPath(command.getOptionValue(CommandOptions.GEOID_PATH.getLongName()));
        }

        if (command.hasOption(CommandOptions.GEOID_PATH.getLongName())) {
            String geoidPath = command.getOptionValue(CommandOptions.GEOID_PATH.getLongName());
            if (geoidPath == null || geoidPath.isEmpty() || geoidPath.equalsIgnoreCase("Ellipsoid")) {
                instance.setGeoidPath(null);
            } else if (geoidPath.equalsIgnoreCase("EGM96")) {
                log.info("Using built-in geoid model: EGM96");

                String resourcePath = "geoid/egm96_15.tif";
                ClassLoader classLoader = GlobalOptions.class.getClassLoader();
                try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        throw new IllegalArgumentException("EGM96 geoid model not found in resources: " + resourcePath);
                    }
                    Path tmp = Files.createTempFile("egm96_15-", ".tif");
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    tmp.toFile().deleteOnExit();
                    instance.setGeoidPath(tmp.toAbsolutePath().toString());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to extract EGM96 geoid model from classpath", e);
                }
            } else {
                instance.setGeoidPath(geoidPath);
            }
        } else {
            instance.setGeoidPath(null);
        }

        if (command.hasOption(CommandOptions.LOG.getLongName())) {
            instance.setLogPath(command.getOptionValue(CommandOptions.LOG.getLongName()));
        }
        instance.setDebugMode(command.hasOption(CommandOptions.DEBUG.getLongName()));
        instance.setLeaveTemp(command.hasOption(CommandOptions.LEAVE_TEMP.getLongName()));
        instance.setContinue(command.hasOption(CommandOptions.CONTINUOUS.getLongName()));
        instance.setOutputCRS(DEFAULT_TARGET_CRS);

        // TODO : Add support for input CRS and tiling schema options
        instance.setTilingSchema(DEFAULT_TILING_SCHEMA);


        if (command.hasOption(CommandOptions.MAXIMUM_TILE_DEPTH.getLongName())) {
            int maxDepth = Integer.parseInt(command.getOptionValue(CommandOptions.MAXIMUM_TILE_DEPTH.getLongName()));
            if (maxDepth < 0) {
                maxDepth = 0;
            } else if (maxDepth > 22) {
                maxDepth = 22;
            }
            instance.setMaximumTileDepth(maxDepth);
        } else {
            instance.setMaximumTileDepth(DEFAULT_MAXIMUM_TILE_DEPTH);
        }

        if (command.hasOption(CommandOptions.MINIMUM_TILE_DEPTH.getLongName())) {
            int minDepth = Integer.parseInt(command.getOptionValue(CommandOptions.MINIMUM_TILE_DEPTH.getLongName()));
            if (minDepth < 0) {
                minDepth = 0;
            } else if (minDepth > 22) {
                minDepth = 22;
            } else if (minDepth > instance.getMaximumTileDepth()) {
                minDepth = 0;
            }
            instance.setMinimumTileDepth(minDepth);
        } else {
            instance.setMinimumTileDepth(DEFAULT_MINIMUM_TILE_DEPTH);
        }

        if (command.hasOption(CommandOptions.JSON.getLongName())) {
            instance.setLayerJsonGenerate(true);
        }

        if (instance.getMinimumTileDepth() > instance.getMaximumTileDepth()) {
            throw new IllegalArgumentException("Minimum tile depth must be less than or equal to maximum tile depth.");
        }

        if (command.hasOption(CommandOptions.INTERPOLATION_TYPE.getLongName())) {
            String interpolationType = command.getOptionValue(CommandOptions.INTERPOLATION_TYPE.getLongName());
            InterpolationType type;
            try {
                type = InterpolationType.fromString(interpolationType);
            } catch (IllegalArgumentException e) {
                log.warn("* Interpolation type is not valid. Set to bilinear.");
                type = DEFAULT_INTERPOLATION_TYPE;
            }
            instance.setInterpolationType(type);
        } else {
            instance.setInterpolationType(DEFAULT_INTERPOLATION_TYPE);
        }

        if (command.hasOption(CommandOptions.PRIORITY_TYPE.getLongName())) {
            String priorityType = command.getOptionValue(CommandOptions.PRIORITY_TYPE.getLongName());
            PriorityType type;
            try {
                type = PriorityType.fromString(priorityType);
            } catch (IllegalArgumentException e) {
                log.warn("* Priority type is not valid. Set to normal.");
                type = PriorityType.RESOLUTION;
            }
            instance.setPriorityType(type);
        } else {
            instance.setPriorityType(PriorityType.RESOLUTION);
        }

        if (command.hasOption(CommandOptions.TILING_MOSAIC_SIZE.getLongName())) {
            instance.setMosaicSize(Integer.parseInt(command.getOptionValue(CommandOptions.TILING_MOSAIC_SIZE.getLongName())));
        } else {
            instance.setMosaicSize(DEFAULT_MOSAIC_SIZE);
        }

        if (command.hasOption(CommandOptions.RASTER_MAXIMUM_SIZE.getLongName())) {
            instance.setMaxRasterSize(Integer.parseInt(command.getOptionValue(CommandOptions.RASTER_MAXIMUM_SIZE.getLongName())));
        } else {
            instance.setMaxRasterSize(DEFAULT_MAX_RASTER_SIZE);
        }

        if (command.hasOption(CommandOptions.INTENSITY.getLongName())) {
            double intensity = Double.parseDouble(command.getOptionValue(CommandOptions.INTENSITY.getLongName()));
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

        if (command.hasOption(CommandOptions.NODATA_VALUE.getLongName())) {
            double noDataValue = Double.parseDouble(command.getOptionValue(CommandOptions.NODATA_VALUE.getLongName()));
            instance.setNoDataValue(noDataValue);
        } else {
            instance.setNoDataValue(DEFAULT_NO_DATA_VALUE);
        }

        instance.setCalculateNormalsExtension(command.hasOption(CommandOptions.EXT_CALCULATE_NORMALS.getLongName()));
        instance.setMetaDataExtension(command.hasOption(CommandOptions.EXT_META_DATA.getLongName()));
        instance.setWaterMaskExtension(command.hasOption(CommandOptions.EXT_WATER_MASK.getLongName()));
        printGlobalOptions();
    }

    protected static void printGlobalOptions() {
        log.info("Input Path: {}", instance.getInputPath());
        log.info("Output Path: {}", instance.getOutputPath());
        log.info("Log Path: {}", instance.getLogPath());
        log.info("Temp Path: {}", instance.getTileTempPath());
        log.info("Geoid Path: {}", instance.getGeoidPath());
        MagoTerrainerMain.drawLine();
        log.info("Layer Json Generate: {}", instance.isLayerJsonGenerate());
        log.info("Tiling Schema: {}", instance.getTilingSchema());
        log.info("Minimum Tile Depth: {}", instance.getMinimumTileDepth());
        log.info("Maximum Tile Depth: {}", instance.getMaximumTileDepth());
        log.info("Interpolation Type: {}", instance.getInterpolationType());
        log.info("Refine Intensity: {}", instance.getIntensity());
        log.info("Priority Type: {}", instance.getPriorityType());
        log.info("NODATA Value: {}", instance.getNoDataValue());
        log.info("Extension Calculate Normals: {}", instance.isCalculateNormalsExtension());
        log.info("Extension Meta Data: {}", instance.isMetaDataExtension());
        log.info("Extension Water Mask: {}", instance.isWaterMaskExtension());
        MagoTerrainerMain.drawLine();
        log.info("Tiling Mosaic Size: {}", instance.getMosaicSize());
        log.info("Tiling Max Raster Size: {}", instance.getMaxRasterSize());
        log.info("Layer Json Generate: {}", instance.isLayerJsonGenerate());
        log.info("Debug Mode: {}", instance.isDebugMode());
        MagoTerrainerMain.drawLine();
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
        title = title == null ? "mago-3d-terrainer" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;

        instance.setStartTime(System.currentTimeMillis());
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }
}
