package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.HeaderStructureAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.UXIssueMessageService;

public class HeaderStructureAuditExecuteTest {

    private HeaderStructureAudit headerStructureAudit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueMessageService;
    private ElementStateService mockElementStateService;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() throws Exception {
        headerStructureAudit = new HeaderStructureAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueMessageService = mock(UXIssueMessageService.class);
        mockElementStateService = mock(ElementStateService.class);
        idCounter = new AtomicLong(1000L);

        Field f1 = HeaderStructureAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(headerStructureAudit, mockAuditService);

        Field f2 = HeaderStructureAudit.class.getDeclaredField("issueMessageService");
        f2.setAccessible(true);
        f2.set(headerStructureAudit, mockIssueMessageService);

        Field f3 = HeaderStructureAudit.class.getDeclaredField("elementStateService");
        f3.setAccessible(true);
        f3.set(headerStructureAudit, mockElementStateService);

        when(mockIssueMessageService.save(any(UXIssueMessage.class))).thenAnswer(inv -> {
            UXIssueMessage msg = inv.getArgument(0);
            msg.setId(idCounter.getAndIncrement());
            return msg;
        });
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PageState createPageState(String html) {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn(html);
        return ps;
    }

    // ---- checkH1Headers tests ----

    @Test
    void testCheckH1Headers_noH1() {
        String html = "<html><body><h2>Sub heading</h2><p>No h1 here</p></body></html>";
        Document doc = Jsoup.parse(html);

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertNull(result, "Should return null when no h1 headers exist");
    }

    @Test
    void testCheckH1Headers_singleH1() {
        String html = "<html><body><h1>Main Title</h1><p>Content</p></body></html>";
        Document doc = Jsoup.parse(html);

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertTrue(result, "Should return true when exactly one h1 header exists");
    }

    @Test
    void testCheckH1Headers_multipleH1() {
        String html = "<html><body><h1>Title One</h1><h1>Title Two</h1><p>Content</p></body></html>";
        Document doc = Jsoup.parse(html);

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertFalse(result, "Should return false when multiple h1 headers exist");
    }

    // ---- findOutOfOrderHeaders tests ----

    @Test
    void testFindOutOfOrderHeaders_ordered() {
        String html = "<html><body><h1>Title</h1><h2>Section</h2><h3>Subsection</h3></body></html>";
        Document doc = Jsoup.parse(html);

        List<Element> outOfOrder = HeaderStructureAudit.findOutOfOrderHeaders(doc);

        assertTrue(outOfOrder.isEmpty(), "Should return empty list when headers are in correct order");
    }

    @Test
    void testFindOutOfOrderHeaders_skippedLevel() {
        String html = "<html><body><h1>Title</h1><h3>Skipped h2</h3></body></html>";
        Document doc = Jsoup.parse(html);

        List<Element> outOfOrder = HeaderStructureAudit.findOutOfOrderHeaders(doc);

        assertFalse(outOfOrder.isEmpty(), "Should find out-of-order headers when levels are skipped");
        assertEquals(1, outOfOrder.size());
        assertEquals("h3", outOfOrder.get(0).tagName());
    }

    @Test
    void testFindOutOfOrderHeaders_noHeaders() {
        String html = "<html><body><p>No headers at all</p><div>Just content</div></body></html>";
        Document doc = Jsoup.parse(html);

        List<Element> outOfOrder = HeaderStructureAudit.findOutOfOrderHeaders(doc);

        assertTrue(outOfOrder.isEmpty(), "Should return empty list when there are no headers");
    }

    // ---- mapHeadersByAncestor tests ----

    @Test
    void testMapHeadersByAncestor_headersInDivs() {
        String html = "<html><body>"
                + "<div id='section1'><h1>Title</h1><h2>Subtitle</h2></div>"
                + "<div id='section2'><h3>Another section</h3></div>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertFalse(result.isEmpty(), "Should have entries in the map");
        // Headers in section1 div should be grouped under their div ancestor
        boolean foundGroupWithTwoHeaders = result.values().stream()
                .anyMatch(list -> list.size() == 2);
        assertTrue(foundGroupWithTwoHeaders, "Should have a group with two headers from the same div");
    }

    @Test
    void testMapHeadersByAncestor_noHeaders() {
        String html = "<html><body><p>No headers</p><div>Content only</div></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertTrue(result.isEmpty(), "Should return empty map when no headers exist");
    }

    // ---- Private method tests via reflection ----

    @Test
    void testIsHeader_viaReflection() throws Exception {
        Method isHeaderMethod = HeaderStructureAudit.class.getDeclaredMethod("isHeader", Element.class);
        isHeaderMethod.setAccessible(true);

        Document doc = Jsoup.parse("<html><body><h1>H1</h1><h6>H6</h6><p>Para</p><div>Div</div></body></html>");

        Element h1 = doc.select("h1").first();
        Element h6 = doc.select("h6").first();
        Element p = doc.select("p").first();
        Element div = doc.select("div").first();

        assertTrue((boolean) isHeaderMethod.invoke(null, h1), "h1 should be recognized as a header");
        assertTrue((boolean) isHeaderMethod.invoke(null, h6), "h6 should be recognized as a header");
        assertFalse((boolean) isHeaderMethod.invoke(null, p), "p should not be recognized as a header");
        assertFalse((boolean) isHeaderMethod.invoke(null, div), "div should not be recognized as a header");
    }

    @Test
    void testFindNearestAncestor_viaReflection() throws Exception {
        Method findNearestAncestorMethod = HeaderStructureAudit.class.getDeclaredMethod("findNearestAncestor", Element.class);
        findNearestAncestorMethod.setAccessible(true);

        Document doc = Jsoup.parse("<html><body><div id='parent'><h2>Heading</h2></div></body></html>");
        Element h2 = doc.select("h2").first();

        Element ancestor = (Element) findNearestAncestorMethod.invoke(null, h2);

        assertNotNull(ancestor, "Should find a nearest ancestor");
        assertEquals("div", ancestor.tagName(), "Nearest non-header ancestor of h2 should be the parent div");
        assertEquals("parent", ancestor.id(), "Should be the div with id 'parent'");
    }

    // ---- execute() integration tests ----

    @Test
    void testExecute_noHeaders() {
        PageState ps = createPageState("<html><body><p>No headers here</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("H1 level header not found")));
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_exactlyOneH1() {
        PageState ps = createPageState("<html><body><h1>Main Title</h1><p>Content</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("exactly 1 H1 header")));
    }

    @Test
    void testExecute_multipleH1s() {
        PageState ps = createPageState("<html><body><h1>Title 1</h1><h1>Title 2</h1></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Too many H1 level headers")));
    }

    @Test
    void testExecute_properHierarchyNoOutOfOrder() {
        PageState ps = createPageState(
                "<html><body><h1>Title</h1><h2>Section</h2><h3>Subsection</h3></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("exactly 1 H1 header")));
        assertFalse(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("not in hierarchical order")));
    }

    @Test
    void testExecute_emptyBody() {
        PageState ps = createPageState("<html><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("H1 level header not found")));
    }

    @Test
    void testExecute_h1AndH2Only() {
        PageState ps = createPageState(
                "<html><body><h1>Title</h1><h2>Section 1</h2><h2>Section 2</h2></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = headerStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertEquals(2, result.getPoints());
        assertEquals(2, result.getTotalPossiblePoints());
    }
}
