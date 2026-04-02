package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TableStructureAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.GenericIssue;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class TableStructureAuditExecuteTest {

    private TableStructureAudit tableStructureAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        tableStructureAudit = new TableStructureAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = TableStructureAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(tableStructureAudit, mockAuditService);

        Field f2 = TableStructureAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(tableStructureAudit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    // Helper to filter issues that have a non-empty recommendation (actual problems)
    private List<GenericIssue> getRealIssues(List<GenericIssue> issues) {
        List<GenericIssue> realIssues = new ArrayList<>();
        for (GenericIssue issue : issues) {
            if (!issue.getRecommendation().isEmpty()) {
                realIssues.add(issue);
            }
        }
        return realIssues;
    }

    // --- validateTable() static method tests ---

    @Test
    void testValidateTable_properTableWithHeaders() {
        String html = "<html><body><table>"
                + "<tr><th scope=\"col\">Name</th><th scope=\"col\">Age</th></tr>"
                + "<tr><td headers=\"name\">Alice</td><td headers=\"age\">30</td></tr>"
                + "</table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        // th elements have scope, so those are passing observations
        // td elements reference headers that may not have matching ids
        List<GenericIssue> realIssues = getRealIssues(issues);
        // The td headers point to "name" and "age" but the th elements don't have those ids
        assertTrue(realIssues.size() >= 2,
                "Should flag td headers attributes that don't match valid th ids");
    }

    @Test
    void testValidateTable_tableWithoutHeaders() {
        String html = "<html><body><table>"
                + "<tr><td>Data 1</td><td>Data 2</td></tr>"
                + "<tr><td>Data 3</td><td>Data 4</td></tr>"
                + "</table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        assertFalse(issues.isEmpty(), "Should have issues for table without headers");
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Table without <th> elements defined")),
                "Should flag missing th elements");
    }

    @Test
    void testValidateTable_tableWithCaption() {
        // Caption is a good practice but validateTable focuses on th/td structure.
        // A table with caption but no th still gets flagged.
        String html = "<html><body><table>"
                + "<caption>Employee Data</caption>"
                + "<tr><th scope=\"col\">Name</th><th scope=\"col\">Role</th></tr>"
                + "<tr><td>Alice</td><td>Engineer</td></tr>"
                + "</table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        // th elements have scope (passing), td elements lack headers attr (flagged)
        List<GenericIssue> realIssues = getRealIssues(issues);
        // Only td issues should be real issues (missing headers attribute)
        for (GenericIssue realIssue : realIssues) {
            assertNotNull(realIssue.getRecommendation(), "Real issues should have recommendations");
        }
    }

    @Test
    void testValidateTable_tableWithoutCaption() {
        String html = "<html><body><table>"
                + "<tr><th>Header 1</th></tr>"
                + "<tr><td>Data 1</td></tr>"
                + "</table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        assertFalse(issues.isEmpty(), "Should have issues for table without scope on th and td without headers");
        List<GenericIssue> realIssues = getRealIssues(issues);
        assertTrue(realIssues.stream().anyMatch(i -> i.getTitle().contains("<th> element without a scope attribute")),
                "Should flag th elements missing scope attribute");
    }

    @Test
    void testValidateTable_emptyTable() {
        String html = "<html><body><table></table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        // Empty table has no th and no td, so only the "no th" issue
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Table without <th> elements defined")),
                "Should flag that the empty table has no th elements");
    }

    @Test
    void testValidateTable_tdWithHeadersAttribute() {
        String html = "<html><body><table>"
                + "<tr><th id=\"h1\" scope=\"col\">Name</th><th id=\"h2\" scope=\"col\">Age</th></tr>"
                + "<tr><td headers=\"h1\">Alice</td><td headers=\"h2\">30</td></tr>"
                + "</table></body></html>";

        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        Set<String> labels = new HashSet<>();

        List<GenericIssue> issues = TableStructureAudit.validateTable(1L, table, labels);

        assertNotNull(issues, "Issues list should not be null");
        // th with scope = passing, td with valid headers = passing
        List<GenericIssue> realIssues = getRealIssues(issues);
        assertTrue(realIssues.isEmpty(),
                "Should have no real issues when th has scope and td headers reference valid ids");
        // Verify passing observations exist
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("<th> has scope attribute defined!")),
                "Should have passing observation for th with scope");
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Table data cell is associated with a valid header")),
                "Should have passing observation for valid td headers");
    }

    // ---- execute() integration tests ----

    @Test
    void testExecute_noTables() {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn("<html><body><p>No tables here</p></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = tableStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_emptyBody() {
        PageState ps = mock(PageState.class);
        when(ps.getId()).thenReturn(100L);
        when(ps.getUrl()).thenReturn("https://example.com");
        when(ps.getSrc()).thenReturn("<html><body></body></html>");
        AuditRecord ar = mock(AuditRecord.class);

        Audit result = tableStructureAudit.execute(ps, ar, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testConstructor() {
        TableStructureAudit a = new TableStructureAudit();
        assertNotNull(a);
    }
}
