package com.gaia3d.wgs84Tiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.reader.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

;

public class TerrainLayer
{
    String tilejson = null;
    String name = null;
    String description = null;
    String version = null;
    String format = null;
    String attribution = null;
    String template = null;
    String legend = null;
    String scheme = null;
    String[] tiles = null;

    String projection = null;
    double[] bounds = null;
    ArrayList<TilesRange> available = new ArrayList<TilesRange>();

    public HashMap<Integer, TilesRange> getTilesRangeMap()
    {
        HashMap<Integer, TilesRange> tilesRangeMap = new HashMap<Integer, TilesRange>();

        for (TilesRange tilesRange : this.available)
        {
            tilesRangeMap.put(tilesRange.tileDepth, tilesRange);
        }

        return tilesRangeMap;
    }

    public void saveJsonFile(String outputDirectory, String tilejsonFileName)
    {
        String fullFileName = outputDirectory + "\\" + tilejsonFileName;
        FileUtils.createAllFoldersIfNoExist(outputDirectory);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();

        objectNodeRoot.put("tilejson", this.tilejson);
        objectNodeRoot.put("name", this.name);
        objectNodeRoot.put("description", this.description);
        objectNodeRoot.put("version", this.version);
        objectNodeRoot.put("format", this.format);
        objectNodeRoot.put("attribution", this.attribution);
        objectNodeRoot.put("template", this.template);
        objectNodeRoot.put("legend", this.legend);
        objectNodeRoot.put("scheme", this.scheme);
        objectNodeRoot.put("projection", this.projection);
        objectNodeRoot.putArray("tiles").add(this.tiles[0]);
        objectNodeRoot.putArray("bounds").add(this.bounds[0]).add(this.bounds[1]).add(this.bounds[2]).add(this.bounds[3]);

        ArrayNode objectNodeAvailable = objectMapper.createArrayNode();
        HashMap<Integer, TilesRange> tilesRangeMap = this.getTilesRangeMap();
        for (Integer tileDepth : tilesRangeMap.keySet())
        {
            TilesRange tilesRange = tilesRangeMap.get(tileDepth);
            ArrayNode objectNodeTileDepth_array = objectMapper.createArrayNode();
            ObjectNode objectNodeTileDepth = objectMapper.createObjectNode();
            objectNodeTileDepth.put("startX", tilesRange.minTileX);
            objectNodeTileDepth.put("endX", tilesRange.maxTileX);
            objectNodeTileDepth.put("startY", tilesRange.minTileY);
            objectNodeTileDepth.put("endY", tilesRange.maxTileY);

            objectNodeTileDepth_array.add(objectNodeTileDepth);
            objectNodeAvailable.add(objectNodeTileDepth_array);

        }

        objectNodeRoot.put("available", objectNodeAvailable);

        // Save the json index file.***
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            objectMapper.writeValue(new File(fullFileName), jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
