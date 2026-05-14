package com.looksee.auditService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageAuditProgressMessage;
import com.looksee.models.message.VerifiedJourneyMessage;

@SpringBootApplication(scanBasePackages = {
    "com.looksee.auditService"
})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * Service-local {@link ObjectMapper} used by the inherited
	 * {@link com.looksee.messaging.web.PubSubAuditController} to deserialize
	 * inbound Pub/Sub payloads. Also used by Spring MVC's HTTP message
	 * converter for {@code @RequestBody Body} binding, so it mirrors the
	 * existing {@code com.looksee.models.config.JacksonConfig} settings:
	 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled
	 * (Pub/Sub push envelopes include a top-level {@code subscription} field
	 * and may include message extras like {@code orderingKey} that {@code Body}
	 * does not model), and {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS}
	 * is disabled to keep {@link java.time.LocalDateTime} payloads ISO-8601.
	 *
	 * <p>Registers a {@link JavaTimeModule} for the {@code LocalDateTime
	 * publishTime} field on {@link Message} and applies a polymorphism mixin
	 * so the abstract {@link Message} type resolves to the correct concrete
	 * subclass based on the {@code messageType} property embedded in every
	 * message body. {@link AuditProgressUpdate} is the {@code defaultImpl}
	 * so legacy traffic missing {@code messageType} still deserializes
	 * through the first handler — same first-attempt behavior the
	 * pre-migration cascading fallback exposed.
	 */
	@Bean
	public ObjectMapper auditServiceObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.addMixIn(Message.class, MessageTypeMixin.class);
		return mapper;
	}

	@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "messageType",
		visible = true,
		defaultImpl = AuditProgressUpdate.class
	)
	@JsonSubTypes({
		@JsonSubTypes.Type(value = AuditProgressUpdate.class,      name = "AuditProgressUpdate"),
		@JsonSubTypes.Type(value = PageAuditProgressMessage.class, name = "PageAuditProgressMessage"),
		@JsonSubTypes.Type(value = JourneyCandidateMessage.class,  name = "JourneyCandidateMessage"),
		@JsonSubTypes.Type(value = VerifiedJourneyMessage.class,   name = "VerifiedJourneyMessage"),
		@JsonSubTypes.Type(value = DiscardedJourneyMessage.class,  name = "DiscardedJourneyMessage")
	})
	private abstract static class MessageTypeMixin {
	}
}
