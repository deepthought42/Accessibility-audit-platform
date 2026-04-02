package com.looksee.journeyExpander;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Spring Boot entry point for the Journey Expander service.
 *
 * <p>Bootstraps the application context, scanning the
 * {@code com.looksee.journeyExpander} package for components, and loads
 * configuration from {@code application.properties}.</p>
 */
@SpringBootApplication(scanBasePackages = {"com.looksee.journeyExpander"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {

	/**
	 * Launches the Spring Boot application.
	 *
	 * @param args command-line arguments forwarded to {@link SpringApplication#run}
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
