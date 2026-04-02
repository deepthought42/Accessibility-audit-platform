package com.looksee.journeyExpander;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ApplicationTest {

    @Test
    void mainMethodInvokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
            springApp.verify(() -> SpringApplication.run(Application.class, new String[]{}));
        }
    }

    @Test
    void applicationClassCanBeInstantiated() {
        Application app = new Application();
        assertNotNull(app);
    }
}
