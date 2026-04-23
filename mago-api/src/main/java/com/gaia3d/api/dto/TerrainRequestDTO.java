package com.gaia3d.api.dto;

import lombok.Data;

@Data
public class TerrainRequestDTO {
    private String input;          // -i, --input
    private String output;         // -o, --output
    private String log;            // -l, --log
    private String temp;           // -t, --temp
    private String geoid;          // -g, --geoid
    private Integer minDepth = 0;  // -min, --minDepth
    private Integer maxDepth = 14; // -max, --maxDepth
    private Double intensity = 4.0;// -is, --intensity
    private String interpolationType = "bilinear"; // -it, --interpolationType
    private String priorityType = "resolution";    // -pt, --priorityType
    private Integer nodataValue = -9999;           // -nv, --nodataValue
    private Boolean calculateNormals = false;      // -cn, --calculateNormals
    private Integer mosaicSize = 16;               // -ms, --mosaicSize
    private Integer rasterMaxSize = 8192;          // -mr, --rasterMaxSize
    private String body = "earth";                 // -b, --body
    private Boolean metadata = false;              // -md, --metadata
    private Boolean waterMask = false;             // -wm, --waterMask
    private Boolean json = false;                  // -j, --json
    private Boolean continueProcess = false;       // -c, --continue
    private Boolean debug = false;                 // -d, --debug
    private Boolean leaveTemp = false;             // -lt, --leaveTemp
}
