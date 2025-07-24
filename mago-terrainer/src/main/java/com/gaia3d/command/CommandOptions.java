package com.gaia3d.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommandOptions {
    /* Default Options */
    HELP("help", "h", false, "Print Help"),
    LEAVE_TEMP("leaveTemp", "lt", false, "Leave temporary files for debugging"),
    JSON("json", "j", false, "Generate layer.json from terrain data"),
    CONTINUOUS("continue", "c", false, "Continue from last terrain generation. This option can be used when terrain creation is interrupted or fails."),

    /* Path Options */
    INPUT("input", "i", true, "[Required] Input directory path"),
    OUTPUT("output", "o", true, "[Required] Output directory path"),
    LOG("log", "l", true, "Log file path"),

    /* Terrain Generate Options */
    MINIMUM_TILE_DEPTH("minDepth", "min", true, "Set minimum terrain tile depth [0 - 22][default : 0]"),
    MAXIMUM_TILE_DEPTH("maxDepth", "max", true, "Set maximum terrain tile depth [0 - 22][default : 14]"),
    INTENSITY("intensity", "is", true, "Set Mesh refinement intensity. [default : 4.0]"),
    INTERPOLATION_TYPE("interpolationType", "it", true, "set Interpolation type [nearest, bilinear][default : bilinear]"),
    PRIORITY_TYPE("priorityType", "pt", true, "Nesting height priority type options [resolution, higher][default : resolution]"),
    NODATA_VALUE("nodataValue", "nv", true, "Set NODATA value for terrain generating [default : -9999]"),
    EXT_CALCULATE_NORMALS("calculateNormals", "cn", false, "Add terrain octVertexNormals for lighting effect"),

    /* Optimize Options */
    TILING_MOSAIC_SIZE("mosaicSize", "ms", true, "Tiling mosaic buffer size per tile. [default : 16]"),
    RASTER_MAXIMUM_SIZE("rasterMaxSize", "mr", true, "Maximum raster size for split function. [default : 8192]"),

    /* Experimental Options */
    //INPUT_CRS("inputCrs", "ic", true, "[Experimental] Input Coordinate Reference System, EPSG Code [4326, 3857...]"),
    //TILING_SCHEMA("tilingSchema", "ts", true, "[Experimental] Schema for the terrain data. [geodetic, mercator][default : geodetic]"),
    //EXT_META_DATA("metadata", "md", false, "[Experimental] Generate metadata for the terrain data."),
    //EXT_WATER_MASK("waterMask", "wm", false, "[Experimental] Generate water mask for the terrain data."),

    /* Debug Options */
    DEBUG("debug", "d", false, "[DEBUG] Print more detailed logs.");

    private final String longName;
    private final String shortName;
    private final boolean argRequired;
    private final String description;

    public static CommandOptions[] getAllOptions() {
        return CommandOptions.values();
    }
}
