package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.OrientationAudit;

public class OrientationTest {
    
    @Test
    public void testNoOrientationRestrictions() throws IOException {
        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body><h1>No Orientation Restrictions</h1></body></html>";
        Document doc = Jsoup.parse(html);

        TestOutputCapture outputCapture = new TestOutputCapture();
        outputCapture.start();

        // Check for orientation restrictions
        OrientationAudit.checkOrientationRestrictions(doc);

        String output = outputCapture.stop();
        assertFalse(output.contains("Warning:"));
    }

    @Test
    public void testOrientationMediaQuery() throws IOException {
        String html = "<html><head><style>"
                + "@media screen and (orientation: landscape) {"
                + "body { background-color: blue; }"
                + "}"
                + "</style></head><body><h1>Landscape Restricted</h1></body></html>";
        Document doc = Jsoup.parse(html);

        TestOutputCapture outputCapture = new TestOutputCapture();
        outputCapture.start();

        // Check for orientation restrictions
        OrientationAudit.checkOrientationRestrictions(doc);

        String output = outputCapture.stop();
        assertTrue(output.contains("Warning: Content may restrict its display orientation"));
    }

    @Test
    public void testOrientationViewportMeta() throws IOException {
        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, orientation=portrait'></head>"
                + "<body><h1>Viewport Orientation Restriction</h1></body></html>";
        Document doc = Jsoup.parse(html);

        TestOutputCapture outputCapture = new TestOutputCapture();
        outputCapture.start();

        // Check for orientation restrictions
        OrientationAudit.checkOrientationRestrictions(doc);

        String output = outputCapture.stop();
        assertTrue(output.contains("Warning: Viewport meta tag suggests possible orientation restriction"));
    }

    @Test
    public void testMultipleOrientationRestrictions() throws IOException {
        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, orientation=landscape'>"
                + "<style>@media screen and (orientation: portrait) {"
                + "body { background-color: red; }"
                + "}"
                + "</style></head><body><h1>Multiple Orientation Restrictions</h1></body></html>";
        Document doc = Jsoup.parse(html);

        TestOutputCapture outputCapture = new TestOutputCapture();
        outputCapture.start();

        // Check for orientation restrictions
        OrientationAudit.checkOrientationRestrictions(doc);

        String output = outputCapture.stop();
        assertTrue(output.contains("Warning: Viewport meta tag suggests possible orientation restriction"));
        assertTrue(output.contains("Warning: Content may restrict its display orientation"));
    }

    @Test
    public void testNoStyleNoMetaTag() throws IOException {
        String html = "<html><body><h1>Content without Style and Meta</h1></body></html>";
        Document doc = Jsoup.parse(html);

        TestOutputCapture outputCapture = new TestOutputCapture();
        outputCapture.start();

        // Check for orientation restrictions
        OrientationAudit.checkOrientationRestrictions(doc);

        String output = outputCapture.stop();
        assertFalse(output.contains("Warning:"));
    }
}

// Helper class to capture system output for testing
class TestOutputCapture {

    private final java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
    private final java.io.PrintStream originalOut = System.out;

    public void start() {
        System.setOut(new java.io.PrintStream(outContent));
    }

    public String stop() {
        System.setOut(originalOut);
        return outContent.toString();
    }
}
