package com.looksee.journeyExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.mapper.Body;
import com.looksee.browser.Browser;
import com.looksee.models.journeys.Step;
import com.looksee.services.IdempotencyService;
import com.looksee.services.StepExecutor;

class AuditControllerTest {

    private void injectIdempotencyService(AuditController controller) throws Exception {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        Field field = AuditController.class.getDeclaredField("idempotencyService");
        field.setAccessible(true);
        field.set(controller, idempotencyService);
    }

    @Test
    void receiveMessageReturnsOkForNullBody() throws Exception {
        AuditController controller = new AuditController();
        injectIdempotencyService(controller);

        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Empty message payload", response.getBody());
    }

    @Test
    void receiveMessageReturnsOkForInvalidPayload() throws Exception {
        AuditController controller = new AuditController();
        injectIdempotencyService(controller);
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn("%%%");

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void safeMessageFallsBackAndTruncates() throws Exception {
        AuditController controller = new AuditController();
        Method method = AuditController.class.getDeclaredMethod("safeMessage", Exception.class);
        method.setAccessible(true);

        String blankResult = (String) method.invoke(controller, new Exception("  "));
        String longResult = (String) method.invoke(controller, new Exception("x".repeat(150)));

        assertEquals("no exception message provided", blankResult);
        assertEquals(100, longResult.length());
    }

    @Test
    void existsInJourneyReturnsTrueWhenMatchingKeyFound() throws Exception {
        AuditController controller = new AuditController();
        Step existingStep = mock(Step.class);
        Step targetStep = mock(Step.class);
        when(existingStep.getKey()).thenReturn("matching-key");
        when(targetStep.getKey()).thenReturn("matching-key");
        List<Step> steps = Arrays.asList(existingStep);

        Method method = AuditController.class.getDeclaredMethod("existsInJourney", List.class, Step.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(controller, steps, targetStep);

        assertTrue(result);
    }

    @Test
    void existsInJourneyReturnsFalseWhenNoMatchingKey() throws Exception {
        AuditController controller = new AuditController();
        Step existingStep = mock(Step.class);
        Step targetStep = mock(Step.class);
        when(existingStep.getKey()).thenReturn("key-a");
        when(targetStep.getKey()).thenReturn("key-b");
        List<Step> steps = Arrays.asList(existingStep);

        Method method = AuditController.class.getDeclaredMethod("existsInJourney", List.class, Step.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(controller, steps, targetStep);

        assertFalse(result);
    }

    @Test
    void performJourneyStepsInBrowserReturnsLatestUrl() throws Exception {
        AuditController controller = new AuditController();
        StepExecutor stepExecutor = mock(StepExecutor.class);
        Field stepExecutorField = AuditController.class.getDeclaredField("step_executor");
        stepExecutorField.setAccessible(true);
        stepExecutorField.set(controller, stepExecutor);

        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.getCurrentUrl()).thenReturn("https://example.com/page-1", "https://example.com/page-2");
        doNothing().when(browser).waitForPageToLoad();

        Step firstStep = mock(Step.class);
        Step secondStep = mock(Step.class);
        List<Step> steps = Arrays.asList(firstStep, secondStep);

        Method method = AuditController.class.getDeclaredMethod("performJourneyStepsInBrowser", List.class, Browser.class);
        method.setAccessible(true);
        String result = (String) method.invoke(controller, steps, browser);

        assertEquals("https://example.com/page-2", result);
    }
}
