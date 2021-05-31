package fr.dossierfacile.common.converter;

import com.google.gson.Gson;
import fr.dossierfacile.common.type.TaxDocument;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class TaxDocumentConverter implements AttributeConverter<TaxDocument, String> {
    private final Gson gson = new Gson();

    @Override
    public String convertToDatabaseColumn(TaxDocument taxDocument) {
        return gson.toJson(taxDocument);
    }

    @Override
    public TaxDocument convertToEntityAttribute(String dbData) {
        return gson.fromJson(dbData, TaxDocument.class);
    }
}
