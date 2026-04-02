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
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserializes a step
 */
public class StepDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
	private static Logger log = LoggerFactory.getLogger(StepDeserializer.class);

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
     * Deserializes a step
     *
     * @param parser the parser to deserialize the step from
     * @param context the deserialization context
     *
     * @return the deserialized step
     *
     * precondition: parser != null
     * precondition: context != null
     */
    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        assert parser != null : "parser must not be null";
        assert context != null : "context must not be null";
        String value = parser.getValueAsString();
        log.warn("value from parser " + parser);
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            if(value.contains("simplestep")) {
            	log.warn("deserializing simple step");
                return objectMapper.readValue(value, SimpleStep.class);
            }
            else if(value.contains("loginstep")) {
                return objectMapper.readValue(value, LoginStep.class);
            }
            else {
            	return value;
            }
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
        /*Base64.Decoder decoder = Base64.getDecoder();

        byte[] decodedValue = decoder.decode(value);

        return new String(decodedValue);
        */
    }
}