package com.looksee.journeyErrors.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.looksee.journeyErrors.models.journeys.SimpleStep;
import com.looksee.journeyErrors.models.journeys.LoginStep;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
	private static Logger log = LoggerFactory.getLogger(StepDeserializer.class);

    private Class<?> resultClass;

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
        this.resultClass = property.getType().getRawClass();
        return this;
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
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