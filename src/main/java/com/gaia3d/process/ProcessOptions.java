package com.gaia3d.process;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions
{
    HELP("help", "h", "help", false, "Print this message"),
    VERSION("version", "v", "version", false, "Print version"),

    INPUT("input", "i", "input", true, "Input folder path"),
    OUTPUT("output", "o", "output", true, "Output folder path"),
    LOG("log", "l", "log", false, "Log file path"),

    MINIMUM_TILE_DEPTH("minDepth", "mn", "minimumTileDepth", true,"Minimum tile depth (0 ~ 22)"),
    MAXIMUM_TILE_DEPTH("maxDepth", "mx", "maximumTileDepth", true,"Maximum tile depth (0 ~ 22)"),
    MESH_REFINEMENT_STRENGTH("strength", "rs", "meshRefinementStrength", false,"Mesh refinement strength");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static ProcessOptions[] getAllOptions() {
        return ProcessOptions.values();
    }
}
