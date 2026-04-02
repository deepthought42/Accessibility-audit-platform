package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.PageLanguageAudit;
import com.looksee.models.audit.messages.UXIssueMessage;

public class LanguageCodeAuditTest {
    @Test
    public void testValidLanguageCodes() {
        // Test a subset of valid language codes
        String[] validCodes = {"en", "fr", "es", "de", "zh"};
        for (String code : validCodes) {
            Assertions.assertTrue(PageLanguageAudit.isValidLanguageCode(code),
                    "Language code should be valid: " + code);
        }
    }

    @Test
    public void testInvalidLanguageCodes() {
        // Test a variety of invalid language codes
        String[] invalidCodes = {"eng", "123", "e", "", "esp", "中文", "рус"};
        for (String code : invalidCodes) {
            Assertions.assertFalse(PageLanguageAudit.isValidLanguageCode(code),
                    "Language code should be invalid: " + code);
        }
    }

    @Test
    public void testLanguageCodeCaseSensitivity() {
        // Valid codes in different cases to test case insensitivity
        Assertions.assertTrue(PageLanguageAudit.isValidLanguageCode("EN"),
                "Language code should be valid when uppercased");
        Assertions.assertTrue(PageLanguageAudit.isValidLanguageCode("eN"),
                "Language code should be valid in mixed case");
    }

    @Test
    public void testNullLanguageCode() {
        // Checking behavior when null is passed
        Assertions.assertFalse(PageLanguageAudit.isValidLanguageCode(null),
                "Language code should be invalid when null");
    }

    @Test
    public void testEmptyLanguageCode() {
        // Checking behavior when an empty string is passed
        Assertions.assertFalse(PageLanguageAudit.isValidLanguageCode(""),
                "Language code should be invalid when empty");
    }

    @Test
    public void testLanguageCodeWithWhitespace() {
        // Checking behavior when code has surrounding whitespace
        Assertions.assertTrue(PageLanguageAudit.isValidLanguageCode(" en "),
                "Language code should be valid even with surrounding whitespace");
    }

    @Test
    void testDocumentWithCorrectLangAttribute() {
        String html = "<html lang='en'><head><title>Test</title></head><body>Content</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = PageLanguageAudit.checkLanguageCompliance(doc);

        assertTrue(issues.isEmpty(), "There should be no issues when the lang attribute is correctly set.");
    }

    @Test
    void testDocumentWithMissingLangAttribute() {
        String html = "<html><head><title>Test</title></head><body>Content</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = PageLanguageAudit.checkLanguageCompliance(doc);

        assertFalse(issues.isEmpty(), "There should be an issue when the lang attribute is missing.");
        assertEquals("Missing Language Attribute", issues.get(0).getTitle(), "The issue should be about the missing language attribute.");
    }

    @Test
    void testDocumentWithInvalidLangAttribute() {
        String html = "<html lang='xx'><head><title>Test</title></head><body>Content</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = PageLanguageAudit.checkLanguageCompliance(doc);

        assertFalse(issues.isEmpty(), "There should be an issue when the lang attribute is invalid.");
        assertEquals("Invalid Language Code", issues.get(0).getTitle(), "The issue should be about the invalid language code.");
    }

    @Test
    void testDocumentWithWhitespaceLangAttribute() {
        String html = "<html lang=' en '><head><title>Test</title></head><body>Content</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = PageLanguageAudit.checkLanguageCompliance(doc);

        assertTrue(issues.isEmpty(), "There should be no issues even when the lang attribute contains whitespace.");
    }
}
