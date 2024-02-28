package fr.dossierfacile.common.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.utils.MapperUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class ParsedFileConverter implements AttributeConverter<ParsedFile, String> {
    private static final ObjectMapper objectMapper = MapperUtil.newObjectMapper();

    @Override
    public String convertToDatabaseColumn(ParsedFile object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting ParsedFile to JSON", e);
        }
    }

    @Override
    public ParsedFile convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // get classification
            JsonNode jsonNode = objectMapper.readTree(dbData);
            ParsedFileClassification classification = ParsedFileClassification.valueOf(jsonNode.get("classification").asText());

            return objectMapper.treeToValue(jsonNode, classification.getClassificationClass());
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to ParsedFile", e);
        }
    }
}
