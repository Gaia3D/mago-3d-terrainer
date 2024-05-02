package com.gaia3d.process;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions
{
    HELP("help", "h", "help", false, "print this message"),
    VERSION("version", "v", "version", false, "print version"),

    INPUT_PATH("inputFolderPath", "i", "inputFolderPath", true, "input folder path"),
    OUTPUT_PATH("outputFolderPath", "o", "outputFolderPath", true, "output folder path"),
    MINIMUM_TILE_DEPTH("minimumTileDepth", "mn", "minimumTileDepth", true,"minimum tile depth"),
    MAXIMUM_TILE_DEPTH("maximumTileDepth", "mx", "maximumTileDepth", true,"maximum tile depth"),
    MESH_REFINEMENT_STRENGTH("meshRefinementStrength", "rs", "meshRefinementStrength", false,"mesh refinement strength");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static ProcessOptions[] getAllOptions() {
        return ProcessOptions.values();
    }
}
