package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import com.looksee.audit.informationArchitecture.audits.LinksAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

public class LinksAuditExecuteTest {

    private LinksAudit audit;
    private PageStateService mockPageStateService;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        audit = new LinksAudit();
        mockPageStateService = mock(PageStateService.class);
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = LinksAudit.class.getDeclaredField("page_state_service");
        f1.setAccessible(true);
        f1.set(audit, mockPageStateService);

        Field f2 = LinksAudit.class.getDeclaredField("audit_service");
        f2.setAccessible(true);
        f2.set(audit, mockAuditService);

        Field f3 = LinksAudit.class.getDeclaredField("issue_message_service");
        f3.setAccessible(true);
        f3.set(audit, mockIssueService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        doNothing().when(mockIssueService).addElement(anyLong(), anyLong());
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Default answer for BrowserUtils static methods - returns sensible defaults
     * for all methods to avoid matcher issues with mockStatic.
     */
    private static Object browserUtilsDefaultAnswer(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com/page";
            case "isJavascript": return false;
            case "doesUrlExist": return true;
            default: return null;
        }
    }

    private static Object browserUtilsDeadLinkAnswer(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com/dead";
            case "isJavascript": return false;
            case "doesUrlExist": return false;
            default: return null;
        }
    }

    private static Object browserUtilsJavascriptAnswer(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com";
            case "isJavascript": return true;
            case "doesUrlExist": return false;
            default: return null;
        }
    }

    private PageState createMockPageState() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(100L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body></body></html>");
        when(pageState.isSecured()).thenReturn(true);
        return pageState;
    }

    private ElementState createMockLinkElement(long id, String outerHtml, String allText) {
        ElementState elem = mock(ElementState.class);
        when(elem.getOuterHtml()).thenReturn(outerHtml);
        when(elem.getId()).thenReturn(id);
        when(elem.getAllText()).thenReturn(allText);
        when(elem.getScreenshotUrl()).thenReturn("https://example.com/screenshot.png");
        return elem;
    }

    @Test
    void testExecute_noLinks() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(Collections.emptyList());

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertEquals(0, result.getPoints());
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_linkWithHref() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(1L,
                "<a href=\"https://example.com/page\">Example</a>", "Example");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getPoints() > 0);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Link has href attribute")));
        }
    }

    @Test
    void testExecute_linkWithoutHref() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(2L,
                "<a>No href link</a>", "No href link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Link is missing href attribute")));
    }

    @Test
    void testExecute_mailtoLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(3L,
                "<a href=\"mailto:test@example.com\">Email us</a>", "Email us");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("mailto")));
        assertEquals(2, result.getPoints());
    }

    @Test
    void testExecute_telLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(4L,
                "<a href=\"tel:+15551234567\">Call us</a>", "Call us");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getDescription().contains("tel: protocol")));
    }

    @Test
    void testExecute_badLinkText() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(6L,
                "<a href=\"https://example.com\">click here</a>", "click here");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Link text is not considered accessible")));
        }
    }

    @Test
    void testExecute_goodLinkText() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(7L,
                "<a href=\"https://example.com\">Visit our homepage</a>", "Visit our homepage");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Link is setup correctly and considered accessible")));
        }
    }

    @Test
    void testExecute_linkWithoutText() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(8L,
                "<a href=\"https://example.com\"></a>", "");
        when(linkElem.getScreenshotUrl()).thenThrow(new RuntimeException("No screenshot"));
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Link is missing text")));
        }
    }

    @Test
    void testExecute_deadLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(9L,
                "<a href=\"https://example.com/dead\">Dead Link</a>", "Dead Link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDeadLinkAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Invalid link url")));
        }
    }

    @Test
    void testExecute_javascriptLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(10L,
                "<a href=\"javascript:void(0)\">JS Link</a>", "JS Link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsJavascriptAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Invalid link url")));
        }
    }

    @Test
    void testExecute_roleNoneLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(11L,
                "<a href=\"https://example.com\" role=\"none\">None Role</a>", "None Role");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getPoints() > 0);
        }
    }

    private static Object browserUtilsMalformedUrlAnswer(InvocationOnMock inv) {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "not a valid url ://broken";
            case "isJavascript": return false;
            case "doesUrlExist": return false;
            default: return null;
        }
    }

    private static Object browserUtilsIOExceptionAnswer(InvocationOnMock inv) throws Exception {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com/page";
            case "isJavascript": return false;
            case "doesUrlExist": throw new java.io.IOException("Connection refused");
            default: return null;
        }
    }

    private static Object browserUtilsExceptionAnswer(InvocationOnMock inv) throws Exception {
        String methodName = inv.getMethod().getName();
        switch (methodName) {
            case "sanitizeUrl": return "https://example.com";
            case "formatUrl": return "https://example.com/page";
            case "isJavascript": return false;
            case "doesUrlExist": throw new RuntimeException("Unexpected error");
            default: return null;
        }
    }

    @Test
    void testExecute_ioExceptionLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(17L,
                "<a href=\"https://example.com/broken\">IO Error Link</a>", "IO Error Link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsIOExceptionAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Invalid link url")));
        }
    }

    @Test
    void testExecute_generalExceptionLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(18L,
                "<a href=\"https://example.com/error\">Error Link</a>", "Error Link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsExceptionAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Invalid link url")));
        }
    }

    @Test
    void testExecute_linkWithItmsAppsProtocol() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(19L,
                "<a href=\"itms-apps://itunes.apple.com/app\">App Store</a>", "App Store");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertFalse(result.getMessages().isEmpty());
        }
    }

    @Test
    void testExecute_emptyHrefLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(15L,
                "<a href=\"\">Empty href</a>", "Empty href");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Link url is missing")));
    }

    @Test
    void testExecute_malformedUrlLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(16L,
                "<a href=\"https://example.com/page\">Some Link</a>", "Some Link");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsMalformedUrlAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Invalid link url format")));
        }
    }

    @Test
    void testExecute_multipleLinks() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState link1 = createMockLinkElement(12L,
                "<a href=\"https://example.com/page1\">Page 1</a>", "Page 1");
        ElementState link2 = createMockLinkElement(13L,
                "<a href=\"https://example.com/page2\">Page 2</a>", "Page 2");
        ElementState link3 = createMockLinkElement(14L,
                "<a>No href</a>", "No href");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(link1, link2, link3));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().size() >= 3);
        }
    }

    @Test
    void testExecute_rolePresentationLink() {
        PageState pageState = createMockPageState();
        AuditRecord auditRecord = mock(AuditRecord.class);
        ElementState linkElem = createMockLinkElement(5L,
                "<a href=\"https://example.com\" role=\"presentation\">Decorative</a>", "Decorative");
        when(mockPageStateService.getLinkElementStates(pageState.getId()))
                .thenReturn(List.of(linkElem));

        try (MockedStatic<BrowserUtils> mock = mockStatic(BrowserUtils.class,
                LinksAuditExecuteTest::browserUtilsDefaultAnswer)) {
            Audit result = audit.execute(pageState, auditRecord, null);

            assertNotNull(result);
            assertTrue(result.getMessages().stream()
                    .anyMatch(msg -> msg.getTitle().contains("Link has href attribute")));
        }
    }
}
