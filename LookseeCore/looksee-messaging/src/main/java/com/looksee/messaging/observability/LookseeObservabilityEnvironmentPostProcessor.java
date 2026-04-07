package com.looksee.messaging.observability;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Injects Looksee's standard observability defaults into every service
 * that depends on the {@code A11yMessaging} module, without requiring each
 * service to maintain its own copy of the same {@code management.*} stanza.
 *
 * <p>Properties are added with the lowest precedence so any service-specific
 * override in {@code application.properties} or environment variables still
 * wins. Registered via {@code META-INF/spring.factories}.</p>
 *
 * <p>Wave 2 of the architecture review.</p>
 */
public class LookseeObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "looksee-observability-defaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(SOURCE_NAME)) {
            return;
        }

        Map<String, Object> defaults = new HashMap<>();

        // Actuator: expose health, info, metrics, prometheus by default. Services
        // can still narrow this list explicitly.
        defaults.put("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        defaults.put("management.endpoint.health.probes.enabled", "true");
        defaults.put("management.endpoint.health.show-details", "when_authorized");

        // Stackdriver / Cloud Monitoring metric export. Disabled until a project
        // ID is provided so local builds and tests do not fail at startup.
        defaults.put("management.metrics.export.stackdriver.enabled",
            "${MANAGEMENT_METRICS_STACKDRIVER_ENABLED:false}");
        defaults.put("management.metrics.export.stackdriver.project-id",
            "${SPRING_CLOUD_GCP_PROJECT_ID:}");
        defaults.put("management.metrics.tags.service",
            "${spring.application.name:unknown-service}");

        // Pub/Sub health indicator currently flaps in test environments; the
        // historical per-service config disabled it. Keep that default here.
        defaults.put("management.health.pubsub.enabled", "false");

        // OpenTelemetry: pick up trace context from incoming Pub/Sub messages
        // and Cloud Run requests. Sampler ratio defaults to 10% to keep cost low
        // and is overridable via env vars.
        defaults.put("otel.service.name", "${spring.application.name:looksee-service}");
        defaults.put("otel.traces.sampler", "${OTEL_TRACES_SAMPLER:parentbased_traceidratio}");
        defaults.put("otel.traces.sampler.arg", "${OTEL_TRACES_SAMPLER_ARG:0.1}");

        environment.getPropertySources()
            .addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
