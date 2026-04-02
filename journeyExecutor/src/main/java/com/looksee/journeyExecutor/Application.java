package com.looksee.journeyExecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@SpringBootApplication(scanBasePackages = {"com.looksee.journeyExecutor"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	public static void main(String[] args)  {
		System.setProperty("webdriver.http.factory", "jdk-http-client");
		SpringApplication.run(Application.class, args);
	}
}
