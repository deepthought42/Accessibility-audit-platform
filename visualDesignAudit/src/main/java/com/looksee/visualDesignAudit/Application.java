package com.looksee.visualDesignAudit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@SpringBootApplication(scanBasePackages = {"com.looksee.visualDesignAudit.audit"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}
}
