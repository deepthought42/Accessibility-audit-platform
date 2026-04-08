package com.looksee.models.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.looksee.browser.enums.BrowserType;
import org.junit.jupiter.api.Test;

class EnumCreateMethodsTest {

    @Test
    void actionCreateSupportsCaseInsensitiveValues() {
        assertEquals(Action.CLICK, Action.create("CLICK"));
        assertEquals(Action.MOUSE_OVER, Action.create("mouseOver"));
        assertEquals("sendKeys", Action.SEND_KEYS.toString());
    }

    @Test
    void actionCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> Action.create(null));
        assertThrows(IllegalArgumentException.class, () -> Action.create("tap"));
    }

    @Test
    void journeyStatusCreateSupportsCaseInsensitiveValues() {
        assertEquals(JourneyStatus.CANDIDATE, JourneyStatus.create("candidate"));
        assertEquals(JourneyStatus.REVIEWING, JourneyStatus.create("REVIEWING"));
        assertEquals("VERIFIED", JourneyStatus.VERIFIED.toString());
    }

    @Test
    void journeyStatusCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> JourneyStatus.create(null));
        assertThrows(IllegalArgumentException.class, () -> JourneyStatus.create("done"));
    }

    @Test
    void auditStatusCreateSupportsCaseInsensitiveValues() {
        assertEquals(AuditStatus.STARTED, AuditStatus.create("started"));
        assertEquals(AuditStatus.COMPLETE, AuditStatus.create("COMPLETE"));
        assertEquals("STOPPED", AuditStatus.STOPPED.toString());
    }

    @Test
    void auditStatusCreateRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> AuditStatus.create("paused"));
    }

    @Test
    void browserTypeCreateSupportsCaseInsensitiveValues() {
        assertEquals(BrowserType.CHROME, BrowserType.create("CHROME"));
        assertEquals(BrowserType.IE, BrowserType.create("ie"));
        assertEquals("firefox", BrowserType.FIREFOX.toString());
    }

    @Test
    void browserTypeCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create(null));
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create("edge"));
    }

    @Test
    void executionStatusCreateSupportsCaseInsensitiveValues() {
        assertEquals(ExecutionStatus.RUNNING, ExecutionStatus.create("RUNNING"));
        assertEquals(ExecutionStatus.RUNNING_AUDITS, ExecutionStatus.create("running audits"));
        assertEquals(ExecutionStatus.EXTRACTING_ELEMENTS, ExecutionStatus.create("extracting_elements"));
        assertEquals("in_progress", ExecutionStatus.IN_PROGRESS.toString());
    }

    @Test
    void executionStatusCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> ExecutionStatus.create(null));
        assertThrows(IllegalArgumentException.class, () -> ExecutionStatus.create("queued"));
    }

    @Test
    void formStatusCreateSupportsCaseInsensitiveValues() {
        assertEquals(FormStatus.DISCOVERED, FormStatus.create("DISCOVERED"));
        assertEquals(FormStatus.ACTION_REQUIRED, FormStatus.create("action_required"));
        assertEquals("classified", FormStatus.CLASSIFIED.toString());
    }

    @Test
    void formStatusCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> FormStatus.create(null));
        assertThrows(IllegalArgumentException.class, () -> FormStatus.create("pending"));
    }


    @Test
    void testStatusCreateSupportsCaseInsensitiveValues() {
        assertEquals(TestStatus.PASSING, TestStatus.create("passing"));
        assertEquals(TestStatus.FAILING, TestStatus.create("FAILING"));
        assertEquals("UNVERIFIED", TestStatus.UNVERIFIED.toString());
    }

    @Test
    void testStatusCreateRejectsNullAndUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> TestStatus.create(null));
        assertThrows(IllegalArgumentException.class, () -> TestStatus.create("blocked"));
    }

}
