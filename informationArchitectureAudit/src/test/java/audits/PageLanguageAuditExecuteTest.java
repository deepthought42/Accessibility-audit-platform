package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.PageLanguageAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.services.AuditService;

public class PageLanguageAuditExecuteTest {

    private PageLanguageAudit audit;
    private AuditService mockAuditService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new PageLanguageAudit();
        mockAuditService = mock(AuditService.class);

        Field f = PageLanguageAudit.class.getDeclaredField("auditService");
        f.setAccessible(true);
        f.set(audit, mockAuditService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testExecute_withValidLang() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html lang=\"en\"><body><p>Hello</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_withMissingLang() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body><p>Hello</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_withInvalidLang() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html lang=\"xyz\"><body><p>Hello</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_withFrenchLang() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html lang=\"fr\"><body><p>Bonjour</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_withEmptyLangAttribute() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html lang=\"\"><body><p>Hello</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_missingHtmlElement() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        // JSoup always wraps content in <html>, so this is hard to trigger
        // But checkLanguageCompliance uses doc.select("html").first()
        // which with Jsoup always returns an element.
        // Let's try a completely empty string
        when(pageState.getSrc()).thenReturn("");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    void testExecute_withRegionalLang() {
        PageState pageState = mock(PageState.class);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html lang=\"en-US\"><body><p>Hello</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    void testIsValidLanguageCode_valid() {
        assertTrue(PageLanguageAudit.isValidLanguageCode("en"));
        assertTrue(PageLanguageAudit.isValidLanguageCode("fr"));
        assertTrue(PageLanguageAudit.isValidLanguageCode("de"));
        assertTrue(PageLanguageAudit.isValidLanguageCode("zh"));
        assertTrue(PageLanguageAudit.isValidLanguageCode("ja"));
    }

    @Test
    void testIsValidLanguageCode_invalid() {
        assertFalse(PageLanguageAudit.isValidLanguageCode("xyz"));
        assertFalse(PageLanguageAudit.isValidLanguageCode(""));
        assertFalse(PageLanguageAudit.isValidLanguageCode(null));
        assertFalse(PageLanguageAudit.isValidLanguageCode("english"));
    }

    @Test
    void testIsValidLanguageCode_caseInsensitive() {
        assertTrue(PageLanguageAudit.isValidLanguageCode("EN"));
        assertTrue(PageLanguageAudit.isValidLanguageCode("Fr"));
    }
}
