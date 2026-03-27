package com.gaia3d.terrain.tile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.util.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
@Setter
@Slf4j
public class TerrainLayer {
    private final List<TileRange> available = new ArrayList<>();
    private String tilejson = null;
    private String name = null;
    private String description = null;
    private String version = null;
    private String format = null;
    private String attribution = null;
    private String template = null;
    private String legend = null;
    private String scheme = null;
    private List<String> extensions = null;
    private String[] tiles = null;
    private String projection = null;
    private double[] bounds = null;

    public TerrainLayer() {
        this.setDefault();
    }

    public HashMap<Integer, TileRange> getTilesRangeMap() {
        HashMap<Integer, TileRange> tilesRangeMap = new HashMap<>();

        for (TileRange tilesRange : this.available) {
            tilesRangeMap.put(tilesRange.getTileDepth(), tilesRange);
        }

        return tilesRangeMap;
    }

    public void setDefault() {
        this.tilejson = "2.1.0";
        this.name = "insert name here";
        this.description = "insert description here";
        this.version = "1.1.0";
        this.format = "quantized-mesh-1.0";
        this.attribution = "insert attribution here";
        this.template = "terrain";
        this.legend = "insert legend here";
        this.scheme = "tms";
        this.tiles = new String[1];
        this.tiles[0] = "{z}/{x}/{y}.terrain?v={version}";
        this.projection = "EPSG:4326"; // CesiumJS TerrainProvider only recognizes EPSG:4326; tile grid is the same angular lon/lat for all bodies
        this.extensions = new ArrayList<>();
        this.bounds = new double[4];
        this.bounds[0] = 0.0;
        this.bounds[1] = 0.0;
        this.bounds[2] = 0.0;
        this.bounds[3] = 0.0;
    }

    public void addExtension(String extension) {
        if (this.extensions == null) {
            this.extensions = new ArrayList<>();
        }
        this.extensions.add(extension);
    }

    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void generateAvailableTiles(String inputPath) {
        File inputDirectory = new File(inputPath);
        if (!inputDirectory.exists()) {
            log.error("Input directory does not exist.");
            return;
        }

        Set<Integer> depthZ = new LinkedHashSet<>();
        File[] depthFiles = inputDirectory.listFiles();
        Arrays.sort(depthFiles);
        for (File depthFile : depthFiles) {
            if (depthFile.isDirectory()) {
                if (!isInteger(depthFile.getName())) {
                    continue;
                }
                Set<Integer> tileX = new LinkedHashSet<>();
                Set<Integer> tileY = new LinkedHashSet<>();
                int tileDepth = Integer.parseInt(depthFile.getName());

                log.info("[Generate][layer.json] Start generating layer.json. tileDepth: {}", tileDepth);
                depthZ.add(tileDepth);
                File[] tileXFiles = depthFile.listFiles();
                for (File tileXFile : tileXFiles) {
                    if (tileXFile.isDirectory()) {
                        if (!isInteger(tileXFile.getName())) {
                            continue;
                        }

                        tileX.add(Integer.parseInt(tileXFile.getName()));
                        File[] tileYFiles = tileXFile.listFiles();
                        for (File tileYFile : tileYFiles) {
                            if (tileYFile.isFile()) {
                                String tileYFileName = tileYFile.getName().split("\\.")[0];
                                if (!isInteger(tileYFileName)) {
                                    continue;
                                }
                                tileY.add(Integer.parseInt(tileYFileName));
                            }
                        }
                    }
                }
                TileRange tilesRange = new TileRange();
                tilesRange.setTileDepth(tileDepth);
                tilesRange.setMinTileX(Collections.min(tileX));
                tilesRange.setMaxTileX(Collections.max(tileX));
                tilesRange.setMinTileY(Collections.min(tileY));
                tilesRange.setMaxTileY(Collections.max(tileY));
                available.add(tilesRange);
            }
        }
        log.info("Available tiles: {}", available);
        log.info("DepthZ: {}", depthZ);
        available.sort(Comparator.comparingInt(TileRange::getTileDepth));

        // calc bounds
        double minLon = -180.0;
        double maxLon = 180.0;
        double minLat = -90.0;
        double maxLat = 90.0;

        TileRange lastTilesRange = available.get(available.size() - 1);
        int lastTileDepth = lastTilesRange.getTileDepth();
        int lastMinTileX = lastTilesRange.getMinTileX();
        int lastMaxTileX = lastTilesRange.getMaxTileX();
        int lastMinTileY = lastTilesRange.getMinTileY();
        int lastMaxTileY = lastTilesRange.getMaxTileY();

        double tileWidth = 360.0 / Math.pow(2, lastTileDepth + 1);
        double tileHeight = 180.0 / Math.pow(2, lastTileDepth);
        double calcMinLon = lastMinTileX * tileWidth + minLon;
        double calcMaxLon = (lastMaxTileX + 1) * tileWidth + minLon;
        double calcMinLat = (lastMaxTileY + 1) * tileHeight + minLat;
        double calcMaxLat = lastMinTileY * tileHeight + minLat;

        log.info("calcMinLon: {}", calcMinLon);
        log.info("calcMaxLon: {}", calcMaxLon);
        log.info("calcMinLat: {}", calcMinLat);
        log.info("calcMaxLat: {}", calcMaxLat);

        minLon = Math.max(minLon, calcMinLon);
        minLat = Math.max(minLat, calcMinLat);
        maxLon = Math.min(maxLon, calcMaxLon);
        maxLat = Math.min(maxLat, calcMaxLat);

        this.bounds[0] = minLon;
        this.bounds[1] = minLat;
        this.bounds[2] = maxLon;
        this.bounds[3] = maxLat;
    }

    public void saveJsonFile(String outputDirectory, String layerJsonName) {
        String fullFileName = outputDirectory + File.separator + layerJsonName;
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

        if (this.extensions != null && this.extensions.size() > 0) {
            ArrayNode objectNodeExtensions = objectMapper.createArrayNode();
            for (String extension : this.extensions) {
                objectNodeExtensions.add(extension);
            }
            objectNodeRoot.set("extensions", objectNodeExtensions);
        }

        ArrayNode objectNodeAvailable = objectMapper.createArrayNode();
        HashMap<Integer, TileRange> tilesRangeMap = this.getTilesRangeMap();
        for (Integer tileDepth : tilesRangeMap.keySet()) {
            TileRange tilesRange = tilesRangeMap.get(tileDepth);
            ArrayNode objectNodeTileDepth_array = objectMapper.createArrayNode();
            ObjectNode objectNodeTileDepth = objectMapper.createObjectNode();
            objectNodeTileDepth.put("startX", tilesRange.getMinTileX());
            objectNodeTileDepth.put("endX", tilesRange.getMaxTileX());
            objectNodeTileDepth.put("startY", tilesRange.getMinTileY());
            objectNodeTileDepth.put("endY", tilesRange.getMaxTileY());

            objectNodeTileDepth_array.add(objectNodeTileDepth);
            objectNodeAvailable.add(objectNodeTileDepth_array);
        }

        objectNodeRoot.set("available", objectNodeAvailable);

        // Save the json index file
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            objectMapper.writeValue(new File(fullFileName), jsonNode);
        } catch (IOException e) {
            log.error("Error:", e);
        }
    }

    public void loadJsonFileCustom(String jsonFullPath, AvailableTileSet availableTileSet) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(new File(jsonFullPath));

            this.tilejson = jsonNode.get("tilejson").asText();
            this.name = jsonNode.get("name").asText();
            this.description = jsonNode.get("description").asText();
            this.version = jsonNode.get("version").asText();
            this.format = jsonNode.get("format").asText();
            this.attribution = jsonNode.get("attribution").asText();
            this.template = jsonNode.get("template").asText();
            this.legend = jsonNode.get("legend").asText();
            this.scheme = jsonNode.get("scheme").asText();
            this.projection = jsonNode.get("projection").asText();

            ArrayNode tilesArrayNode = (ArrayNode) jsonNode.get("tiles");
            if (tilesArrayNode != null && tilesArrayNode.size() > 0) {
                this.tiles = new String[tilesArrayNode.size()];
                for (int i = 0; i < tilesArrayNode.size(); i++) {
                    this.tiles[i] = tilesArrayNode.get(i).asText();
                }
            }

            ArrayNode boundsArrayNode = (ArrayNode) jsonNode.get("bounds");
            if (boundsArrayNode != null && boundsArrayNode.size() == 4) {
                this.bounds = new double[4];
                for (int i = 0; i < boundsArrayNode.size(); i++) {
                    this.bounds[i] = boundsArrayNode.get(i).asDouble();
                }
            }

            ArrayNode extensionsArrayNode = (ArrayNode) jsonNode.get("extensions");
            if (extensionsArrayNode != null && extensionsArrayNode.size() > 0) {
                this.extensions = new ArrayList<>();
                for (int i = 0; i < extensionsArrayNode.size(); i++) {
                    this.extensions.add(extensionsArrayNode.get(i).asText());
                }
            }

            // available
            Map<Integer, List<TileRange>> mapDepthAvailableTileRanges = availableTileSet.getMapDepthAvailableTileRanges();
            ArrayNode availableArrayNode = (ArrayNode) jsonNode.get("available");
            if (availableArrayNode != null && availableArrayNode.size() > 0) {
                for (int i = 0; i < availableArrayNode.size(); i++) {
                    ArrayNode tileDepthArrayNode = (ArrayNode) availableArrayNode.get(i);
                    if (tileDepthArrayNode != null && tileDepthArrayNode.size() > 0) {
                        List<TileRange> tileRanges = new ArrayList<>();
                        for (int j = 0; j < tileDepthArrayNode.size(); j++) {
                            JsonNode tileDepthNode = tileDepthArrayNode.get(j);
                            int startX = tileDepthNode.get("startX").asInt();
                            int endX = tileDepthNode.get("endX").asInt();
                            int startY = tileDepthNode.get("startY").asInt();
                            int endY = tileDepthNode.get("endY").asInt();
                            TileRange tileRange = new TileRange();
                            tileRange.setMinTileX(startX);
                            tileRange.setMaxTileX(endX);
                            tileRange.setMinTileY(startY);
                            tileRange.setMaxTileY(endY);
                            tileRanges.add(tileRange);
                        }
                        mapDepthAvailableTileRanges.put(i, tileRanges);
                    }
                }
            }

        } catch (IOException e) {
            log.error("Error:", e);
        }
    }

    public void saveJsonFileCustom(String outputDirectory, String layerJsonName, AvailableTileSet availableTileSet) {
        String fullFileName = outputDirectory + File.separator + layerJsonName;
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

        if (this.extensions != null && this.extensions.size() > 0) {
            ArrayNode objectNodeExtensions = objectMapper.createArrayNode();
            for (String extension : this.extensions) {
                objectNodeExtensions.add(extension);
            }
            objectNodeRoot.set("extensions", objectNodeExtensions);
        }

        ArrayNode objectNodeAvailable = objectMapper.createArrayNode();
        Map<Integer, List<TileRange>> mapDepthAvailableTileRanges = availableTileSet.getMapDepthAvailableTileRanges();
        for (Integer tileDepth : mapDepthAvailableTileRanges.keySet()) {
            List<TileRange> tileRanges = mapDepthAvailableTileRanges.get(tileDepth);
            ArrayNode objectNodeTileDepth_array = objectMapper.createArrayNode();
            for (TileRange tilesRange : tileRanges) {
                ObjectNode objectNodeTileDepth = objectMapper.createObjectNode();
                objectNodeTileDepth.put("startX", tilesRange.getMinTileX());
                objectNodeTileDepth.put("endX", tilesRange.getMaxTileX());
                objectNodeTileDepth.put("startY", tilesRange.getMinTileY());
                objectNodeTileDepth.put("endY", tilesRange.getMaxTileY());
                objectNodeTileDepth_array.add(objectNodeTileDepth);
            }
            objectNodeAvailable.add(objectNodeTileDepth_array);
        }

        objectNodeRoot.set("available", objectNodeAvailable);

        // Save the json index file
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            objectMapper.writeValue(new File(fullFileName), jsonNode);
        } catch (IOException e) {
            log.error("Error:", e);
        }
    }

}
