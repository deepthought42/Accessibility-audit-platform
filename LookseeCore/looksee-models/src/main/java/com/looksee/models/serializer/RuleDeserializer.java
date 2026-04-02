package com.looksee.models.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.looksee.models.rules.Rule;
import com.looksee.models.rules.RuleFactory;
import java.io.IOException;
import lombok.NoArgsConstructor;

/**
 * A deserializer for {@link Rule} objects
 */
@NoArgsConstructor
public class RuleDeserializer extends JsonDeserializer<Rule> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Rule deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode node = oc.readTree(jp);
		final String type = node.get("type").asText();
		final String value = node.get("value").asText();
		return RuleFactory.build(type, value);
	}
}