package com.looksee.journeyExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ApplicationTest {
    @Test
    void mainSetsWebdriverHttpFactoryProperty() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
            assertEquals("jdk-http-client", System.getProperty("webdriver.http.factory"));
            springApplication.verify(() -> SpringApplication.run(Application.class, new String[]{}));
        }
    }
}
