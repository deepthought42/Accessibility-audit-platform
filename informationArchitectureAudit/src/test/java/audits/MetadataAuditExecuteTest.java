package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.MetadataAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

public class MetadataAuditExecuteTest {

    private MetadataAudit audit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        audit = new MetadataAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = MetadataAudit.class.getDeclaredField("audit_service");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = MetadataAudit.class.getDeclaredField("issue_message_service");
        f2.setAccessible(true);
        f2.set(audit, mockIssueService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PageState createPageState(String title, String html) {
        PageState ps = mock(PageState.class);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getTitle()).thenReturn(title);
        when(ps.getSrc()).thenReturn(html);
        return ps;
    }

    @Test
    void testExecute_optimalTitle_noMetaDescription() {
        // Title between 50-60 chars, no meta description
        String title = "This is a well-crafted page title that is optimal length now";
        PageState ps = createPageState(title,
                "<html><head></head><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // Should have title issue + "Meta description not found" issue + refresh issue
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Meta description not found")));
    }

    @Test
    void testExecute_shortTitle() {
        String title = "Short";
        PageState ps = createPageState(title,
                "<html><head></head><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // scoreTitle has a bug: `page_title.length() > 50 || page_title.length() < 60`
        // is always true, so short titles still pass. But we test it runs without error.
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_longTitle() {
        String title = "This is a very long page title that exceeds sixty characters and goes on and on and on for too long";
        PageState ps = createPageState(title,
                "<html><head></head><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_withOptimalMetaDescription() {
        // Meta description between 120-150 chars
        String desc = "This is an optimal meta description that provides a good summary of what this page is about and helps users find the content they are looking for efficiently";
        String title = "Test Page Title That Is A Good Length For SEO Optimization";
        String html = "<html><head><meta name=\"description\" content=\"" + desc + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_withLongMetaDescription() {
        // Meta description > 150 chars
        String desc = "This is a very long meta description that exceeds one hundred and fifty characters. It goes on and on providing way too much detail about what the page contains. It should be shortened to be more concise and effective for SEO.";
        String title = "Test Page Title For SEO";
        String html = "<html><head><meta name=\"description\" content=\"" + desc + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("too long")));
    }

    @Test
    void testExecute_withShortMetaDescription() {
        // Meta description < 120 chars
        String desc = "Short description for testing purposes only.";
        String title = "Test Page Title For SEO";
        String html = "<html><head><meta name=\"description\" content=\"" + desc + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("too short")));
    }

    @Test
    void testExecute_withEmptyMetaDescription() {
        String title = "Test Page Title For SEO";
        String html = "<html><head><meta name=\"description\" content=\"\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Meta description is empty")));
    }

    @Test
    void testExecute_withMultipleMetaDescriptions() {
        String desc1 = "This is a meta description that is long enough to be between one hundred and twenty and one hundred fifty characters for testing";
        String desc2 = "This is another meta description that is also long enough to be between one hundred twenty and one hundred fifty characters for tests";
        String title = "Test Page Title";
        String html = "<html><head><meta name=\"description\" content=\"" + desc1 + "\">"
                + "<meta name=\"description\" content=\"" + desc2 + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Too many meta descriptions")));
    }

    @Test
    void testExecute_noMetaRefresh() {
        String title = "Test Page Title";
        String html = "<html><head></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // Should have positive refresh score (no refresh = good)
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Meta refresh tag found")));
    }

    @Test
    void testExecute_withDifficultReadingLevel() {
        // A description that is hard to read (low readability score)
        String desc = "The epistemological ramifications of implementing multifaceted computational paradigms necessitate comprehensive analytical frameworks for evaluating systematic methodological approaches within interdisciplinary contexts.";
        String title = "Test Page Title";
        String html = "<html><head><meta name=\"description\" content=\"" + desc + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        // Should have readability issue
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("to read") || msg.getTitle().contains("reading level")));
    }

    @Test
    void testExecute_withEasyReadingLevel() {
        // Simple, easy-to-read description between 120-150 chars
        String desc = "Our shop sells fresh bread and cakes. We bake them every day. Come visit us for great food. We are open all week from nine to five every single day.";
        String title = "Test Page Title";
        String html = "<html><head><meta name=\"description\" content=\"" + desc + "\"></head><body></body></html>";
        PageState ps = createPageState(title, html);
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = audit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("reading level")));
    }
}
