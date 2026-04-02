package com.looksee.audit.informationArchitecture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@SpringBootApplication(scanBasePackages = {"com.looksee.audit.informationArchitecture"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {

	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}

}
