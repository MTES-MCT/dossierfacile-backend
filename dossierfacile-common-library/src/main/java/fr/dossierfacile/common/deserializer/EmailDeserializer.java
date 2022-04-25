package fr.dossierfacile.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Locale;

public class EmailDeserializer extends StdDeserializer<String> {

    public EmailDeserializer() {
        this(null);
    }

    public EmailDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jsonparser, DeserializationContext context) throws IOException {
        return (jsonparser.getText() == null) ? null : jsonparser.getText().toLowerCase(Locale.ROOT);
    }
}