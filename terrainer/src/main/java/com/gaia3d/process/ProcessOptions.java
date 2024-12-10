package com.gaia3d.process;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions {
    HELP("help", "h", "help", false, "Print this message"),

    INPUT("input", "i", "input", true, "Input folder path"),
    OUTPUT("output", "o", "output", true, "Output folder path"),
    LOG("log", "l", "log", true, "Log file path"),
    DEBUG("debug", "d", "debug", false, "Debug Mode, print more detail log"),
    JSON("json", "j", "json", false, "Generate only layer.json from terrain data"),

    MINIMUM_TILE_DEPTH("minDepth", "mn", "minDepth", true,"Minimum tile depth (range : 0 ~ 22) (default : 0)"),
    MAXIMUM_TILE_DEPTH("maxDepth", "mx", "maxDepth", true,"Maximum tile depth (range : 0 ~ 22) (default : 18)"),
    INTENSITY("intensity", "rs", "intensity", true,"Mesh refinement strength. (default : 1.0)"),
    CALCULATE_NORMALS("calculateNormals", "cn", "calculateNormals", false, "Calculate normals"),
    INTERPOLATION_TYPE("interpolationType", "it", "interpolationType", true, "Interpolation type (nearest, bilinear)"),
    TILING_MOSAIC_SIZE("mosaicSize", "ms", "mosaicSize", true, "Tiling mosaic size per tile (default : 32)"),
    RASTER_MAXIMUM_SIZE("rasterMaxSize", "mr", "rasterMaxSize", true, "Maximum raster size per tile (default : 8192)");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static ProcessOptions[] getAllOptions() {
        return ProcessOptions.values();
    }
}
