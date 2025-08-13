package fr.dossierfacile.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.exceptions.ParsingException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Converter
public class ListOfRulesJsonConverter implements AttributeConverter<List<DocumentAnalysisRule>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<DocumentAnalysisRule> list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed to convert list to json", e);
        }
    }

    @Override
    public List<DocumentAnalysisRule> convertToEntityAttribute(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(data, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new ParsingException("Failed to convert json to list", e);
        }
    }

}