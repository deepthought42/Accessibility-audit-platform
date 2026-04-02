package com.looksee.contentAudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@SpringBootApplication(scanBasePackages = {"com.looksee.contentAudit"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}

}
