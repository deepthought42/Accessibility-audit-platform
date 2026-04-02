package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import com.looksee.audit.informationArchitecture.audits.TitleAndHeaderAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

public class TitleAndHeaderAuditExecuteTest {

    private TitleAndHeaderAudit audit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        audit = new TitleAndHeaderAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = TitleAndHeaderAudit.class.getDeclaredField("audit_service");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = TitleAndHeaderAudit.class.getDeclaredField("issue_message_service");
        f2.setAccessible(true);
        f2.set(audit, mockIssueService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Object browserUtilsWithTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return "My Page Title";
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com";
            case "isJavascript": return false;
            case "doesUrlExist": return true;
            default: return null;
        }
    }

    private static Object browserUtilsNoTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return "";
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com";
            case "isJavascript": return false;
            case "doesUrlExist": return true;
            default: return null;
        }
    }

    private static Object browserUtilsNullTitle(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "getTitle": return null;
            case "sanitizeUrl": return "https://example.com";
            default: return null;
        }
    }

    private PageState createPageStateWithHtml(String html) {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn(html);
        return ps;
    }

    // --- scorePageTitles tests ---
    // Note: scorePageTitles calls BrowserUtils.getTitle which requires Selenium Grid
    // classes (org.openqa.grid) not available in the test classpath. These tests verify
    // the method exists and is accessible but cannot be invoked without the dependency.

    @Test
    void testScorePageTitles_methodExists() throws Exception {
        Method method = TitleAndHeaderAudit.class.getDeclaredMethod("scorePageTitles", PageState.class);
        method.setAccessible(true);
        assertNotNull(method);
    }

    // --- scoreFavicon tests ---

    private Score invokeScoreFavicon(PageState pageState) throws Exception {
        Method method = TitleAndHeaderAudit.class.getDeclaredMethod("scoreFavicon", PageState.class);
        method.setAccessible(true);
        return (Score) method.invoke(audit, pageState);
    }

    @Test
    void testScoreFavicon_withFavicon() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn("<html><head><link rel=\"icon\" href=\"/favicon.ico\"></head><body></body></html>");

        Score score = invokeScoreFavicon(pageState);

        assertNotNull(score);
        assertEquals(1, score.getPointsAchieved());
        assertTrue(score.getIssueMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Favicon is present")));
    }

    @Test
    void testScoreFavicon_withoutFavicon() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn("<html><head></head><body></body></html>");

        Score score = invokeScoreFavicon(pageState);

        assertNotNull(score);
        assertEquals(0, score.getPointsAchieved());
        assertTrue(score.getIssueMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("favicon is missing")));
    }

    @Test
    void testScoreFavicon_withShortcutIcon() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn("<html><head><link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\"></head><body></body></html>");

        Score score = invokeScoreFavicon(pageState);

        assertNotNull(score);
        assertEquals(1, score.getPointsAchieved());
        assertEquals(1, score.getMaxPossiblePoints());
    }

    // --- scoreTextElementHeaders tests ---

    private Score invokeScoreTextElementHeaders(PageState pageState) throws Exception {
        Method method = TitleAndHeaderAudit.class.getDeclaredMethod("scoreTextElementHeaders", PageState.class);
        method.setAccessible(true);
        return (Score) method.invoke(audit, pageState);
    }

    @Test
    void testScoreTextElementHeaders_withHeadersBeforeText() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn(
                "<html><body><h1>Title</h1><p>Some paragraph text</p></body></html>");

        Score score = invokeScoreTextElementHeaders(pageState);

        assertNotNull(score);
        assertTrue(score.getMaxPossiblePoints() >= 0);
    }

    @Test
    void testScoreTextElementHeaders_noHeadings() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn(
                "<html><body><p>Just text without headers</p></body></html>");

        Score score = invokeScoreTextElementHeaders(pageState);

        assertNotNull(score);
    }

    @Test
    void testScoreTextElementHeaders_emptyBody() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn("<html><body></body></html>");

        Score score = invokeScoreTextElementHeaders(pageState);

        assertNotNull(score);
        assertEquals(0, score.getPointsAchieved());
        assertEquals(0, score.getMaxPossiblePoints());
    }

    @Test
    void testScoreTextElementHeaders_headingsOnly() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn(
                "<html><body><h1>Title</h1><h2>Subtitle</h2></body></html>");

        Score score = invokeScoreTextElementHeaders(pageState);

        assertNotNull(score);
        assertEquals(0, score.getPointsAchieved());
        assertEquals(0, score.getMaxPossiblePoints());
    }

    // --- scoreOrderedListHeaders tests ---

    private Score invokeScoreOrderedListHeaders(PageState pageState) throws Exception {
        Method method = TitleAndHeaderAudit.class.getDeclaredMethod("scoreOrderedListHeaders", PageState.class);
        method.setAccessible(true);
        return (Score) method.invoke(audit, pageState);
    }

    @Test
    void testScoreOrderedListHeaders_withList() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn(
                "<html><body><h2>My List</h2><ol><li>Item 1</li><li>Item 2</li></ol></body></html>");

        Score score = invokeScoreOrderedListHeaders(pageState);

        assertNotNull(score);
    }

    @Test
    void testScoreOrderedListHeaders_noLists() throws Exception {
        PageState pageState = mock(PageState.class);
        when(pageState.getSrc()).thenReturn("<html><body><p>No lists</p></body></html>");

        Score score = invokeScoreOrderedListHeaders(pageState);

        assertNotNull(score);
        assertEquals(0, score.getPointsAchieved());
    }

    // ---- execute() integration tests ----

    @Test
    void testExecute_withTitleAndFavicon() {
        PageState ps = createPageStateWithHtml(
                "<html><head><link rel=\"icon\" href=\"/favicon.ico\"></head><body><h1>Title</h1><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Page has a title")));
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Favicon is present")));
        }
    }

    @Test
    void testExecute_noTitleNoFavicon() {
        PageState ps = createPageStateWithHtml(
                "<html><head></head><body><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsNoTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("missing a title")));
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("favicon is missing")));
        }
    }

    @Test
    void testExecute_nullTitle() {
        PageState ps = createPageStateWithHtml(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsNullTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("missing a title")));
        }
    }

    @Test
    void testExecute_withHeadingsAndTextContent() {
        PageState ps = createPageStateWithHtml(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head>" +
                "<body><h1>Main</h1><div><h2>Section</h2><div>Some text content here</div></div></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_withOrderedList() {
        PageState ps = createPageStateWithHtml(
                "<html><head><link rel=\"icon\" href=\"/fav.ico\"></head>" +
                "<body><h1>Title</h1><ol><li>Item 1</li><li>Item 2</li></ol><p>Some text</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_textWithHeaderSiblingBefore() {
        PageState ps = createPageStateWithHtml(
                "<html><head></head><body>" +
                "<h2>Section Header</h2>" +
                "<div>Text block following header</div>" +
                "</body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_textWithHeaderSiblingAfter() {
        PageState ps = createPageStateWithHtml(
                "<html><head></head><body>" +
                "<div>Text block before header</div>" +
                "<h2>Section Header</h2>" +
                "</body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                TitleAndHeaderAuditExecuteTest::browserUtilsWithTitle)) {
            Audit result = audit.execute(ps, ar, null);

            assertNotNull(result);
        }
    }
}
