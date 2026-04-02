package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.ListStructureAudit;
import com.looksee.audit.informationArchitecture.models.WcagEmphasisComplianceAudit;

public class WCAGListComplianceCheckerTest {

    @Test
    public void testCheckListCompliance_ValidLists() {
        // Test case: HTML document with valid <ul> and <ol> elements
        String htmlContent = "<html><body><ul><li>Item 1</li><li>Item 2</li></ul>"
                            + "<ol><li>Step 1</li><li>Step 2</li></ol></body></html>";
        Document doc = Jsoup.parse(htmlContent);

        List<String> cssSelectors = ListStructureAudit.checkListCompliance(doc);
        
        // Expecting true because all lists are properly structured
        assertTrue(cssSelectors.isEmpty());
    }

    @Test
    public void testCheckListCompliance_InvalidLists() {
        // Test case: HTML document with invalid <ul> element containing non-<li> children
        String htmlContent = "<html><body><ul><li>Item 1</li><div>Invalid Item</div></ul></body></html>";
        Document doc = Jsoup.parse(htmlContent);

        List<String> cssSelectors = ListStructureAudit.checkListCompliance(doc);

        // Expecting false because the <ul> contains a <div> instead of <li> elements only
        assertFalse(cssSelectors.isEmpty());
    }

    @Test
    public void testCheckListCompliance_NoLists() {
        // Test case: HTML document without any <ul> or <ol> elements
        String htmlContent = "<html><body><p>No lists here</p></body></html>";
        Document doc = Jsoup.parse(htmlContent);

        List<String> cssSelectors = ListStructureAudit.checkListCompliance(doc);

        // Expecting true because there are no lists to check, so it's technically compliant
        assertTrue(cssSelectors.isEmpty());
    }

    @Test
    public void testAreChildrenListItems_AllListItems() {
        // Test case: Valid <ul> element with all <li> children
        String htmlContent = "<ul><li>Item 1</li><li>Item 2</li></ul>";
        Document doc = Jsoup.parse(htmlContent);
        Element ulElement = doc.select("ul").first();

        boolean result = ListStructureAudit.areChildrenListItems(ulElement);

        // Expecting true because all children are <li> elements
        assertTrue(result);
    }

    @Test
    public void testAreChildrenListItems_NonListItemChildren() {
        // Test case: Invalid <ul> element with a non-<li> child
        String htmlContent = "<ul><li>Item 1</li><div>Invalid Item</div></ul>";
        Document doc = Jsoup.parse(htmlContent);
        Element ulElement = doc.select("ul").first();

        boolean result = ListStructureAudit.areChildrenListItems(ulElement);

        // Expecting false because the <ul> contains a <div> instead of <li> elements only
        assertFalse(result);
    }

    @Test
    public void testAreChildrenListItems_NullElement() {
        // Test case: Passing a null element, which should trigger an assertion error
        assertThrows(NullPointerException.class, () -> ListStructureAudit.areChildrenListItems(null));
    }

    @Test
    public void testAreChildrenListItems_NonListElement() {
        // Test case: Passing an element that is not a <ul> or <ol>, which should trigger an assertion error
        String htmlContent = "<p>This is not a list</p>";
        Document doc = Jsoup.parse(htmlContent);
        Element pElement = doc.select("p").first();

        assertThrows(IllegalArgumentException.class, () -> ListStructureAudit.areChildrenListItems(pElement));
    }

    @Test
    public void testCheckSpecialTextCompliance() {
        // Test HTML content
        String htmlContent = "<html><body>"
                           + "<p>This is a <strong>strong</strong> emphasis.</p>"
                           + "<p>This is <code>code</code> snippet.</p>"
                           + "<p>This is an <abbr title=\"abbreviation\">abbr</abbr> example.</p>"
                           + "<p>This is an <abbr>missing title</abbr> example.</p>"
                           + "<blockquote>This is a blockquote.</blockquote>"
                           + "<p>This is <b>bold</b> and <i>italic</i> text.</p>"
                           + "</body></html>";

        Document doc = Jsoup.parse(htmlContent);

        List<String> nonCompliantSelectors = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        
        // Expected non-compliant selectors
        List<String> expectedNonCompliantSelectors = Arrays.asList(
            "html > body > p:nth-child(1) > strong",
            "html > body > p:nth-child(2) > code",
            "html > body > blockquote",
            "html > body > p:nth-child(4) > abbr"
            
        );

        assertEquals(expectedNonCompliantSelectors, nonCompliantSelectors, "The non-compliant selectors should match.");
    }

    @Test
    public void testCheckEmphasisCompliance() {
        // Test HTML content
        String htmlContent = "<html><body>"
                           + "<p>This is a <strong>strong</strong> emphasis.</p>"
                           + "<p>This is <b>bold</b> and <i>italic</i> text.</p>"
                           + "</body></html>";

        Document doc = Jsoup.parse(htmlContent);

        List<String> complianceResults = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);

        for(String selector: complianceResults){
            System.out.println("selector : "+selector);
        }

        // Expected compliance results
        // In this case, <b> and <i> are non-compliant
        List<String> expectedNonCompliantSelectors = Arrays.asList(
            "html > body > p:nth-child(2) > b",
            "html > body > p:nth-child(2) > i"
        );

        assertEquals(expectedNonCompliantSelectors, complianceResults, "The non-compliant emphasis elements should match.");
    }
}