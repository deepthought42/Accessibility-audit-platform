package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.ListStructureAudit;

public class ListStructureAuditTest {

    // --- checkListCompliance tests ---

    @Test
    void checkListCompliance_compliantUlWithOnlyLiChildren() {
        String html = "<html><body><ul><li>Item 1</li><li>Item 2</li></ul></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertTrue(nonCompliant.isEmpty());
    }

    @Test
    void checkListCompliance_nonCompliantUlWithDivChildren() {
        String html = "<html><body><ul><div>Not a list item</div></ul></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertEquals(1, nonCompliant.size());
    }

    @Test
    void checkListCompliance_compliantOlWithOnlyLiChildren() {
        String html = "<html><body><ol><li>First</li><li>Second</li><li>Third</li></ol></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertTrue(nonCompliant.isEmpty());
    }

    @Test
    void checkListCompliance_nonCompliantOlWithMixedChildren() {
        String html = "<html><body><ol><li>First</li><span>Not an LI</span></ol></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertEquals(1, nonCompliant.size());
    }

    @Test
    void checkListCompliance_nestedLists() {
        // Nested ul inside li is compliant - the outer ul only has li children
        String html = "<html><body><ul><li>Item 1<ul><li>Sub 1</li></ul></li><li>Item 2</li></ul></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertTrue(nonCompliant.isEmpty());
    }

    @Test
    void checkListCompliance_emptyList() {
        String html = "<html><body><ul></ul></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        // Empty list has no children, so areChildrenListItems returns true
        assertTrue(nonCompliant.isEmpty());
    }

    @Test
    void checkListCompliance_documentWithNoLists() {
        String html = "<html><body><p>No lists here</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> nonCompliant = ListStructureAudit.checkListCompliance(doc);

        assertTrue(nonCompliant.isEmpty());
    }

    // --- areChildrenListItems tests ---

    @Test
    void areChildrenListItems_validList() {
        String html = "<ul><li>A</li><li>B</li></ul>";
        Document doc = Jsoup.parse(html);
        Element ul = doc.select("ul").first();

        assertTrue(ListStructureAudit.areChildrenListItems(ul));
    }

    @Test
    void areChildrenListItems_invalidList() {
        String html = "<ul><li>A</li><div>B</div></ul>";
        Document doc = Jsoup.parse(html);
        Element ul = doc.select("ul").first();

        assertFalse(ListStructureAudit.areChildrenListItems(ul));
    }

    @Test
    void areChildrenListItems_emptyUl() {
        String html = "<ul></ul>";
        Document doc = Jsoup.parse(html);
        Element ul = doc.select("ul").first();

        assertTrue(ListStructureAudit.areChildrenListItems(ul));
    }

    @Test
    void areChildrenListItems_olWithOnlyLiChildren() {
        String html = "<ol><li>One</li><li>Two</li></ol>";
        Document doc = Jsoup.parse(html);
        Element ol = doc.select("ol").first();

        assertTrue(ListStructureAudit.areChildrenListItems(ol));
    }

    @Test
    void areChildrenListItems_olWithNonLiChild() {
        String html = "<ol><li>One</li><p>Two</p></ol>";
        Document doc = Jsoup.parse(html);
        Element ol = doc.select("ol").first();

        assertFalse(ListStructureAudit.areChildrenListItems(ol));
    }
}
