package fr.dossierfacile.common.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;

@Converter
public class ListStringConverter implements AttributeConverter<List<String>, String> {
    private final Gson gson = new Gson();

    @Override
    public String convertToDatabaseColumn(List<String> strings) {
        return gson.toJson(strings);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return gson.fromJson(dbData, new TypeToken<List<String>>() {
        }.getType());
    }
}
