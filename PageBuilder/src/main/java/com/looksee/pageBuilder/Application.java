package com.looksee.pageBuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


/**
 * Spring Boot entry point for the PageBuilder microservice.
 *
 * <p>Configures the WebDriver HTTP factory system property and launches the
 * Spring application context.</p>
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>The {@code webdriver.http.factory} system property is set to
 *       {@code "jdk-http-client"} after {@link #main(String[])} executes.</li>
 * </ul>
 */
@SpringBootApplication( scanBasePackages = {
	"com.looksee.pageBuilder"
})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {

	/**
	 * Launches the PageBuilder application.
	 *
	 * <h4>Preconditions</h4>
	 * <ul>
	 *   <li>{@code args} must not be {@code null}.</li>
	 * </ul>
	 *
	 * <h4>Postconditions</h4>
	 * <ul>
	 *   <li>System property {@code webdriver.http.factory} is set to
	 *       {@code "jdk-http-client"}.</li>
	 *   <li>The Spring application context is started.</li>
	 * </ul>
	 *
	 * @param args command-line arguments forwarded to Spring Boot
	 */
	public static void main(String[] args)  {
		assert args != null : "Command-line arguments array must not be null";

		System.setProperty("webdriver.http.factory", "jdk-http-client");

		assert "jdk-http-client".equals(System.getProperty("webdriver.http.factory"))
				: "Postcondition failed: webdriver.http.factory was not set";

		SpringApplication.run(Application.class, args);
	}
}
