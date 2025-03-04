package com.gaia3d.command;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommandOptions {
    HELP("help", "h", "help", false, "Print this message"),

    INPUT("input", "i", "input", true, "Input folder path"),
    OUTPUT("output", "o", "output", true, "Output folder path"),
    LOG("log", "l", "log", true, "Log file path"),
    DEBUG("debug", "d", "debug", false, "Debug Mode, print more detail log"),
    LEAVE_TEMP("leaveTemp", "lt", "leaveTemp", false, "Leave temporary files for debugging"),
    JSON("json", "j", "json", false, "Generate only layer.json from terrain data"),

    // for terrain generation
    MINIMUM_TILE_DEPTH("minDepth", "min", "minDepth", true,"Minimum tile depth (range : 0 ~ 22) (default : 0)"),
    MAXIMUM_TILE_DEPTH("maxDepth", "max", "maxDepth", true,"Maximum tile depth (range : 0 ~ 22) (default : 14)"),
    INTENSITY("intensity", "is", "intensity", true,"Mesh refinement strength. (default : 4.0)"),
    CALCULATE_NORMALS("calculateNormals", "cn", "calculateNormals", false, "Add terrain octVertexNormals for lighting effect"),
    INTERPOLATION_TYPE("interpolationType", "it", "interpolationType", true, "Interpolation type (nearest, bilinear) (default : bilinear)"),
    PRIORITY_TYPE("priorityType", "pt", "priorityType", true, "Priority type () (default : distance)"),

    // for optimization
    TILING_MOSAIC_SIZE("mosaicSize", "ms", "mosaicSize", true, "Tiling mosaic buffer size per tile. (default : 16)"),
    RASTER_MAXIMUM_SIZE("rasterMaxSize", "mr", "rasterMaxSize", true, "Maximum raster size for split function. (default : 8192)");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static CommandOptions[] getAllOptions() {
        return CommandOptions.values();
    }
}
