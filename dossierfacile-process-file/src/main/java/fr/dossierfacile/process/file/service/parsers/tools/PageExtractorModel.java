package fr.dossierfacile.process.file.service.parsers.tools;

import com.fasterxml.jackson.databind.JsonNode;
import fr.dossierfacile.common.utils.MapperUtil;
import fr.dossierfacile.process.file.service.parsers.AbstractPDFParser;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PageExtractorModel {
    private final JsonNode jsonNode;

    public int getMaxPageCount() {
        return jsonNode.get("page").get("maxPageCount").asInt(1);
    }

    public record Zone(String name, Rectangle rect, String regexp, String pageFilter) {
    }

    public PageExtractorModel(String jsonResourceModelPath) throws IOException {
        this.jsonNode = MapperUtil.newObjectMapper().readTree(AbstractPDFParser.class.getResource(jsonResourceModelPath));
    }

    public double getDefaultWidth() {
        return jsonNode.get("page").get("width").asDouble();
    }

    public String getBackgroundImageMD5() {
        return jsonNode.get("classification").get("background-image-md5").asText();
    }

    public Map<String, Rectangle> getNamedZones(double scale) {
        Map<String, Rectangle> map = new TreeMap();
        JsonNode attributesNode = jsonNode.get("zones");
        for (JsonNode attributeNode : attributesNode) {
            String rectStr = attributeNode.get("rect").asText();
            String[] rectSplit = rectStr.split(",");
            Rectangle rectangle = new Rectangle((int) (Integer.parseInt(rectSplit[0]) * scale), (int) (Integer.parseInt(rectSplit[1]) * scale), (int) (Integer.parseInt(rectSplit[2]) * scale), (int) (Integer.parseInt(rectSplit[3]) * scale));

            map.put(attributeNode.get("name").asText(), rectangle);
        }
        return map;
    }

    public List<Zone> getMatchingZones(double scale) {
        JsonNode attributesNode = jsonNode.get("matchingZones");
        if (attributesNode != null) {
            List<Zone> list = new LinkedList<>();
            for (JsonNode attributeNode : attributesNode) {
                String rectStr = attributeNode.get("rect").asText();
                String[] rectSplit = rectStr.split(",");
                Rectangle rectangle = new Rectangle((int) (Integer.parseInt(rectSplit[0]) * scale), (int) (Integer.parseInt(rectSplit[1]) * scale), (int) (Integer.parseInt(rectSplit[2]) * scale), (int) (Integer.parseInt(rectSplit[3]) * scale));

                list.add(new Zone(attributeNode.get("name").asText(), rectangle, attributeNode.get("matches").asText(), null));
            }
            return list;
        }
        return null;
    }
}
