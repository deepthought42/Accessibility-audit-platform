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
 * Custom JSON deserializer that handles base64 encoded JSON strings.
 * This deserializer decodes base64 strings and then deserializes them as JSON
 * into the target class type. It implements ContextualDeserializer to access
 * the target class information during deserialization.
 */
public class Base64JsonDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private Class<?> resultClass;

    /**
     * 
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
        assert context != null : "context must not be null";
        assert property != null : "property must not be null";

        this.resultClass = property.getType().getRawClass();
        return this;
    }

    /**
     * Deserializes a base64 encoded JSON string into the target class type.
     *
     * @param parser the JSON parser
     * @param context the deserialization context
     * @return the deserialized object
     * @throws IOException if an I/O error occurs
     * @throws JsonProcessingException if a JSON processing error occurs
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