package com.looksee.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.util.Base64;

/**
 * A custom deserializer for base64 encoded JSON
 */
public class Base64Deserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private Class<?> resultClass;

    /**
     * Creates a contextual deserializer for the given property
     * @param context the deserialization context
     * @param property the property to deserialize
     * @return the contextual deserializer
     * @throws JsonMappingException if the property is not a valid type
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
        assert context != null : "context must not be null";
        assert property != null : "property must not be null";

        this.resultClass = property.getType().getRawClass();
        return this;
    }

    /**
     * Deserializes a base64 encoded JSON string
     * @param parser the JSON parser
     * @param context the deserialization context
     * @return the deserialized object
     * @throws IOException if an I/O error occurs
     * @throws JsonProcessingException if a JSON processing error occurs
     * @throws InvalidFormatException if the value is not a valid base64 encoded JSON
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

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] decodedValue = decoder.decode(value);

            return objectMapper.readValue(decodedValue, this.resultClass);
        } catch (IllegalArgumentException | JsonParseException e) {
            String fieldName = parser.getParsingContext().getCurrentName();
            Class<?> wrapperClass = parser.getParsingContext().getCurrentValue().getClass();

            throw new InvalidFormatException(
                parser,
                String.format("Value for '%s' is not a base64 encoded JSON", fieldName),
                value,
                wrapperClass
            );
        }
    }
}