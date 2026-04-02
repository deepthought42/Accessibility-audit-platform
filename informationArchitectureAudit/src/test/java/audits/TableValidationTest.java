package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TableStructureAudit;
import com.looksee.models.audit.GenericIssue;

public class TableValidationTest {

    @Test
    public void testTableWithoutThElements() {
        String html = "<html><body><table><tr><td>Data</td></tr></table></body></html>";
        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        long page_state_id = 1;

        List<GenericIssue> result = TableStructureAudit.validateTable(page_state_id, table, new HashSet<>());

        assertEquals(2, result.size());
        assertEquals("Table without <th> elements defined", result.get(0).getTitle());
        assertEquals("No headers attribute was found for <td> element", result.get(1).getTitle());
    }

    @Test
    public void testTableWithThElementsAndScope() {
        String html = "<html><body><table><tr><th scope='col'>Header</th><td>Data</td></tr></table></body></html>";
        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        long page_state_id = 1;
        List<GenericIssue> result = TableStructureAudit.validateTable(page_state_id, table, new HashSet<>());
        List<GenericIssue> realIssues = getRealIssues(result);

        assertEquals(1, realIssues.size());
        assertEquals("No headers attribute was found for <td> element", realIssues.get(0).getTitle());
    }

    @Test
    public void testTableWithThElementsWithoutScope() {
        String html = "<html><body><table><tr><th>Header</th><td>Data</td></tr></table></body></html>";
        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        long page_state_id = 1;

        List<GenericIssue> result = TableStructureAudit.validateTable(page_state_id, table, new HashSet<>());

        assertEquals(2, result.size());
        assertEquals("<th> element without a scope attribute", result.get(0).getTitle());
        assertEquals("No headers attribute was found for <td> element", result.get(1).getTitle());
    }

    @Test
    public void testTableWithTdHeadersAttribute() {
        String html = "<html><body><table><tr><th id='header1'>Header</th></tr><tr><td >Data</td></tr></table></body></html>";
        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        long page_state_id = 1;

        List<GenericIssue> result = TableStructureAudit.validateTable(page_state_id, table, new HashSet<>());

        assertEquals(2, result.size());
        assertEquals("<th> element without a scope attribute", result.get(0).getTitle());
        assertEquals("No headers attribute was found for <td> element", result.get(1).getTitle());
    }

    @Test
    public void testTableWithInvalidTdHeadersAttribute() {
        String html = "<html><body><table><tr><th id='header1'>Header</th><th id='header2' scope='header2'>Header2</th></tr><tr><td headers='header2'>Data</td><td>Data2</td></tr></table></body></html>";
        Document doc = Jsoup.parse(html);
        Element table = doc.select("table").first();
        long page_state_id = 1;

        List<GenericIssue> result = TableStructureAudit.validateTable(page_state_id, table, new HashSet<>());
        List<GenericIssue> realIssues = getRealIssues(result);
        assertEquals(2, realIssues.size());
        assertEquals("<th> element without a scope attribute", realIssues.get(0).getTitle());
        assertEquals("No headers attribute was found for <td> element", realIssues.get(1).getTitle());
    }

    public List<GenericIssue> getRealIssues(List<GenericIssue> issues){
        List<GenericIssue> real_issues = new ArrayList<>();
        for(GenericIssue issue : issues){
            if(!issue.getRecommendation().isEmpty()){
                real_issues.add(issue);
            }
        }
        return real_issues;
    }
}
