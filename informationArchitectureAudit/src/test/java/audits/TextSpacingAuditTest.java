package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TextSpacingAudit;
import com.looksee.models.ElementState;
import com.looksee.models.audit.messages.UXIssueMessage;

public class TextSpacingAuditTest {
    @Test
    public void testEvaluateTextSpacing_CompliantElement() {
        List<ElementState> elements = new ArrayList<>();
        elements.add(createElementState("16px", "24px", "2px", "4px", "32px"));

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);

        assertTrue(issues.isEmpty(), "No issues should be found for compliant elements.");
    }

    @Test
    public void testEvaluateTextSpacing_NonCompliantLineHeight() {
        List<ElementState> elements = new ArrayList<>();
        elements.add(createElementState("16px", "20px", "2px", "4px", "32px"));

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);

        assertEquals(1, issues.size(), "One issue should be found for non-compliant line height.");
        assertEquals("Insufficient line height", issues.get(0).getTitle(), "The issue title should be 'Insufficient Line Height'.");
    }

    @Test
    public void testEvaluateTextSpacing_NonCompliantLetterSpacing() {
        List<ElementState> elements = new ArrayList<>();
        elements.add(createElementState("16px", "24px", "1px", "4px", "32px"));

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);

        assertEquals(1, issues.size(), "One issue should be found for non-compliant letter spacing.");
        assertEquals("Insufficient letter spacing", issues.get(0).getTitle(), "The issue title should be 'Insufficient Letter Spacing'.");
    }

    @Test
    public void testEvaluateTextSpacing_NonCompliantWordSpacing() {
        List<ElementState> elements = new ArrayList<>();
        elements.add(createElementState("16px", "24px", "2px", "2px", "32px"));

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);

        assertEquals(1, issues.size(), "One issue should be found for non-compliant word spacing.");
        assertEquals("Insufficient word spacing", issues.get(0).getTitle(), "The issue title should be 'Insufficient Word Spacing'.");
    }

    @Test
    public void testEvaluateTextSpacing_NonCompliantParagraphSpacing() {
        List<ElementState> elements = new ArrayList<>();
        elements.add(createElementState("16px", "24px", "2px", "4px", "20px"));

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);

        assertEquals(1, issues.size(), "One issue should be found for non-compliant paragraph spacing.");
        assertEquals("Insufficient paragraph spacing", issues.get(0).getTitle(), "The issue title should be 'Insufficient Paragraph Spacing'.");
    }

    @Test
    public void testParseCssValue_HandlesVariousUnits() {
        assertEquals(16.0, TextSpacingAudit.parseCssValue("16px"), 0.01, "16px should equal 16.0 pixels");
        assertEquals(32.0, TextSpacingAudit.parseCssValue("2em"), 0.01, "2em should equal 32.0 pixels");
        assertEquals(24.0, TextSpacingAudit.parseCssValue("18pt"), 0.01, "18pt should equal 24.0 pixels");
        assertEquals(3.2, TextSpacingAudit.parseCssValue("20%"), 0.01, "20% should equal 3.2 pixels");
        assertEquals(37.795, TextSpacingAudit.parseCssValue("1cm"), 0.01, "1cm should equal 37.795 pixels");
        assertEquals(3.7795, TextSpacingAudit.parseCssValue("1mm"), 0.01, "1mm should equal 3.7795 pixels");
        assertEquals(96.0, TextSpacingAudit.parseCssValue("1in"), 0.01, "1in should equal 96.0 pixels");
        assertEquals(16.0, TextSpacingAudit.parseCssValue("1pc"), 0.01, "1pc should equal 16.0 pixels");
        assertEquals(8.0, TextSpacingAudit.parseCssValue("1ex"), 0.01, "1ex should equal 8.0 pixels");
    }

    // Helper method to create an ElementState with specific CSS values
    private ElementState createElementState(String fontSize, String lineHeight, String letterSpacing, String wordSpacing, String paragraphSpacing) {
        ElementState element = new ElementState();
        element.getRenderedCssValues().put("font-size", fontSize);
        element.getRenderedCssValues().put("line-height", lineHeight);
        element.getRenderedCssValues().put("letter-spacing", letterSpacing);
        element.getRenderedCssValues().put("word-spacing", wordSpacing);
        element.getRenderedCssValues().put("margin-bottom", paragraphSpacing);
        return element;
    }
}
