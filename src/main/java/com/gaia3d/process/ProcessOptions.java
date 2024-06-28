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

    MINIMUM_TILE_DEPTH("minDepth", "mn", "minDepth", true,"Minimum tile depth (0 ~ 22)"),
    MAXIMUM_TILE_DEPTH("maxDepth", "mx", "maxDepth", true,"Maximum tile depth (0 ~ 22)"),
    MESH_REFINEMENT_STRENGTH("strength", "rs", "meshRefinementStrength", false,"Mesh refinement strength"),
    CALCULATE_NORMALS("calculateNormals", "cn", "calculateNormals", false, "Calculate normals"),
    INTERPOLATION_TYPE("interpolationType", "it", "interpolationType", true, "Interpolation type (nearest, bilinear, bicubic)"),
    TILING_MOSAIC_SIZE("mosaicSize", "ms", "mosaicSize", true, "Tiling mosaic size (default : 16)");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static ProcessOptions[] getAllOptions() {
        return ProcessOptions.values();
    }
}
