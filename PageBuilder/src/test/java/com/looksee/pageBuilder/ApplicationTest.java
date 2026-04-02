package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApplicationTest {

    @Test
    void mainSetsWebdriverProperty() {
        // We cannot fully start the Spring context without external dependencies,
        // but we can verify that the system property is set by calling main
        // and catching the expected context-startup failure.
        try {
            Application.main(new String[]{
                    "--spring.main.web-application-type=none",
                    "--server.port=0",
                    "--management.server.port=0"
            });
        } catch (Exception e) {
            // Expected: Spring context may fail due to missing beans/config
        }
        assertEquals("jdk-http-client", System.getProperty("webdriver.http.factory"));
    }

    @Test
    void applicationClassCanBeInstantiated() {
        Application app = new Application();
        assertNotNull(app);
    }
}
