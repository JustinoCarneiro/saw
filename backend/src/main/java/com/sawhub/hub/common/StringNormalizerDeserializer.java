package com.sawhub.hub.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.Locale;

@JsonComponent
public class StringNormalizerDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }

        // 1. Remove espaços em branco nas pontas
        value = value.trim();

        // 2. Se o nome do campo se referir a um e-mail, converte tudo para minúsculo
        String fieldName = p.getParsingContext().getCurrentName();
        if (fieldName != null && fieldName.toLowerCase(Locale.ROOT).contains("email")) {
            value = value.toLowerCase(Locale.ROOT);
        }

        return value;
    }
}
