package com.gaia3d.process;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions
{
    HELP("help", "h", "help", false, "print this message"),
    VERSION("version", "v", "version", false, "print version"),

    INPUT_FOLDER_PATH("inputFolderPath", "in", "inputFolderPath", true, "input folder path"),
    OUTPUT_FOLDER_PATH("outputFolderPath", "out", "outputFolderPath", true, "output folder path"),
    MINIMUM_TILE_DEPTH("minimumTileDepth", "min", "minimumTileDepth", true,"minimum tile depth"),
    MAXIMUM_TILE_DEPTH("maximumTileDepth", "max", "maximumTileDepth", true,"maximum tile depth"),
    MESH_REFINEMENT_STRENGTH("meshRefinementStrength", "refinementStrength", "meshRefinementStrength", false,"mesh refinement strength");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;

    public static ProcessOptions[] getAllOptions() {
        return ProcessOptions.values();
    }
}
