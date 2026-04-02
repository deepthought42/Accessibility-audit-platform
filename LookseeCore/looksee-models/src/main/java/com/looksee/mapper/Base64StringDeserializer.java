package com.looksee.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;
import java.util.Base64;

/**
 * Deserializes a base64 string
 */
public class Base64StringDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private Class<?> resultClass;

    /**
     * Creates a contextual deserializer
     *
     * @param context the deserialization context
     * @param property the property to deserialize
     *
     * @return the contextual deserializer
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
        assert context != null : "context must not be null";
        assert property != null : "property must not be null";

        this.resultClass = property.getType().getRawClass();
        return this;
    }

    /**
     * Deserializes a base64 string
     *
     * @param parser the parser to deserialize the base64 string from
     * @param context the deserialization context
     *
     * @return the deserialized base64 string
     *
     * precondition: parser != null
     * precondition: context != null
     */
    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        assert parser != null : "parser must not be null";
        assert context != null : "context must not be null";
        String value = parser.getValueAsString();
        Base64.Decoder decoder = Base64.getDecoder();

        byte[] decodedValue = decoder.decode(value);

        return new String(decodedValue);
    }
}