package com.looksee.llm.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.llm.safety.RedactionReport.Category;
import org.junit.jupiter.api.Test;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @Test
    void redactsEmail() {
        PiiRedactor.Result r = redactor.redact("Contact me at alice@example.com please");
        assertTrue(r.text().contains("[REDACTED:EMAIL]"));
        assertFalse(r.text().contains("alice@example.com"));
        assertEquals(1, r.report().getCounts().get(Category.EMAIL));
    }

    @Test
    void redactsUsPhone() {
        PiiRedactor.Result r = redactor.redact("Call (415) 555-1234 now");
        assertTrue(r.text().contains("[REDACTED:PHONE]"));
    }

    @Test
    void redactsValidCreditCard() {
        // 4111 1111 1111 1111 is the canonical Luhn-valid Visa test number.
        PiiRedactor.Result r = redactor.redact("card 4111111111111111 end");
        assertTrue(r.text().contains("[REDACTED:CC]"));
    }

    @Test
    void doesNotRedactLuhnInvalidDigits() {
        PiiRedactor.Result r = redactor.redact("order 1234567890123456 end");
        assertTrue(r.text().contains("1234567890123456"));
    }

    @Test
    void redactsSsn() {
        PiiRedactor.Result r = redactor.redact("ssn 123-45-6789");
        assertTrue(r.text().contains("[REDACTED:SSN]"));
    }

    @Test
    void redactsJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abcDEF123";
        PiiRedactor.Result r = redactor.redact("token=" + jwt);
        assertTrue(r.text().contains("[REDACTED:JWT]"));
    }

    @Test
    void redactsApiKey() {
        PiiRedactor.Result r = redactor.redact("api_key=sk_live_abcdefghijklmnopqrstuv");
        assertTrue(r.text().contains("[REDACTED:API_KEY]"));
    }

    @Test
    void redactsHtmlPasswordInputValue() {
        String html = "<input type=\"password\" value=\"hunter2\" name=\"pw\">";
        PiiRedactor.Result r = redactor.redact(html);
        assertFalse(r.text().contains("hunter2"));
        assertTrue(r.text().contains("[REDACTED]"));
    }

    @Test
    void redactsUrlQuerySecret() {
        PiiRedactor.Result r = redactor.redact("https://api.example.com/x?access_token=abc123&q=foo");
        assertTrue(r.text().contains("access_token=[REDACTED]"));
        assertTrue(r.text().contains("q=foo"));
    }

    @Test
    void emptyInputReturnsEmptyReport() {
        PiiRedactor.Result r = redactor.redact("");
        assertEquals(0, r.report().total());
    }

    @Test
    void passesThroughCleanText() {
        String clean = "The quick brown fox jumps over the lazy dog.";
        PiiRedactor.Result r = redactor.redact(clean);
        assertEquals(clean, r.text());
        assertEquals(0, r.report().total());
    }
}
