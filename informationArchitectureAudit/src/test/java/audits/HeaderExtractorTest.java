package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.HeaderStructureAudit;

public class HeaderExtractorTest {

    @Test
    public void testMapHeadersByAncestor_singleHeader() {
        String html = "<html><body><h1>Header 1</h1></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertEquals(1, result.size(), "Expected exactly one group of headers.");
        Element body = doc.body();
        assertTrue(result.containsKey(body), "Expected the body element to be the common ancestor.");
        assertEquals(1, result.get(body).size(), "Expected one header under the body.");
        assertEquals("Header 1", result.get(body).get(0).text(), "Expected header text to match 'Header 1'.");
    }

    @Test
    public void testMapHeadersByAncestor_multipleHeadersSameAncestor() {
        String html = "<html><body><div><h1>Header 1</h1><h2>Header 2</h2></div></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertEquals(1, result.size(), "Expected exactly one group of headers.");
        Element div = doc.select("div").first();
        assertTrue(result.containsKey(div), "Expected the <div> element to be the common ancestor.");
        assertEquals(2, result.get(div).size(), "Expected two headers under the <div>.");
        assertEquals("Header 1", result.get(div).get(0).text(), "Expected header text to match 'Header 1'.");
        assertEquals("Header 2", result.get(div).get(1).text(), "Expected header text to match 'Header 2'.");
    }

    @Test
    public void testMapHeadersByAncestor_multipleHeadersDifferentAncestors() {
        String html = "<html><body><div><h1>Header 1</h1></div><section><h2>Header 2</h2><h3>Header 3</h3></section><div><h2>header 2_1</h2><div></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertEquals(3, result.size(), "Expected two groups of headers.");
        Element div = doc.select("div").first();
        Element section = doc.select("section").first();

        assertTrue(result.containsKey(div), "Expected the <div> element to be an ancestor.");
        assertEquals(1, result.get(div).size(), "Expected one header under the <div>.");
        assertEquals("Header 1", result.get(div).get(0).text(), "Expected header text to match 'Header 1'.");

        assertTrue(result.containsKey(section), "Expected the <section> element to be an ancestor.");
        assertEquals(2, result.get(section).size(), "Expected one header under the <section>.");
        assertEquals("Header 2", result.get(section).get(0).text(), "Expected header text to match 'Header 2'.");
    }

    @Test
    public void testMapHeadersByAncestor_nestedHeaders() {
        String html = "<html><body><div><h1>Header 1</h1><div><h2>Header 2</h2></div></div></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertEquals(2, result.size(), "Expected two groups of headers.");
        Element outerDiv = doc.select("div").first();
        Element innerDiv = doc.select("div").get(1);

        assertTrue(result.containsKey(outerDiv), "Expected the outer <div> element to be an ancestor.");
        assertEquals(1, result.get(outerDiv).size(), "Expected one header under the outer <div>.");
        assertEquals("Header 1", result.get(outerDiv).get(0).text(), "Expected header text to match 'Header 1'.");

        assertTrue(result.containsKey(innerDiv), "Expected the inner <div> element to be an ancestor.");
        assertEquals(1, result.get(innerDiv).size(), "Expected one header under the inner <div>.");
        assertEquals("Header 2", result.get(innerDiv).get(0).text(), "Expected header text to match 'Header 2'.");
    }

    @Test
    public void testMapHeadersByAncestor_noHeaders() {
        String html = "<html><body><div>No headers here</div></body></html>";
        Document doc = Jsoup.parse(html);

        Map<Element, List<Element>> result = HeaderStructureAudit.mapHeadersByAncestor(doc);

        assertTrue(result.isEmpty(), "Expected no groups of headers.");
    }

    @Test
    public void testFindOutOfOrderHeaders_detectsSkippedLevels() {
        String html = "<html><body><h1>Title</h1><h2>Section</h2><h4>Skipped</h4></body></html>";
        Document doc = Jsoup.parse(html);

        List<Element> result = HeaderStructureAudit.findOutOfOrderHeaders(doc);

        assertEquals(1, result.size(), "Expected one out-of-order header when levels are skipped.");
        assertEquals("Skipped", result.get(0).text(), "Expected the skipped-level heading to be reported.");
    }

    @Test
    public void testFindOutOfOrderHeaders_allowsSequentialOrDescendingLevels() {
        String html = "<html><body><h1>Title</h1><h2>Section</h2><h3>Sub</h3><h2>Next Section</h2></body></html>";
        Document doc = Jsoup.parse(html);

        List<Element> result = HeaderStructureAudit.findOutOfOrderHeaders(doc);

        assertTrue(result.isEmpty(), "Expected no out-of-order headers for sequential/descending levels.");
    }

}
