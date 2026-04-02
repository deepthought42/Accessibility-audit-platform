package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.AudioControlAudit;
import com.looksee.models.audit.GenericIssue;

public class AudioControlAuditTest {

    @Test
    void testAudioElementWithAutoplayAndNoControls() {
        String html = "<html><body>"
                + "<audio autoplay><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size());
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
        assertTrue(issues.get(0).getDescription().contains("Autoplaying audio or video found without user controls or mute options."));
    }

    @Test
    void testVideoElementWithAutoplayAndNoControls() {
        String html = "<html><body>"
                + "<video autoplay><source src='video.mp4' type='video/mp4'></video>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size());
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
        assertTrue(issues.get(0).getDescription().contains("Autoplaying audio or video found without user controls or mute options."));
    }

    @Test
    void testAudioElementWithAutoplayAndControls() {
        String html = "<html><body>"
                + "<audio autoplay controls><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(0, issues.size());  // No issues should be found
    }

    @Test
    void testVideoElementWithAutoplayAndMuted() {
        String html = "<html><body>"
                + "<video autoplay muted><source src='video.mp4' type='video/mp4'></video>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(0, issues.size());  // No issues should be found
    }

    @Test
    void testIframeWithAutoplayContent() {
        String html = "<html><body>"
                + "<iframe src='https://example.com/video?autoplay=1'></iframe>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size());
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
        assertTrue(issues.get(0).getDescription().contains("Embedded content with autoplaying audio or video found."));
    }

    @Test
    void testNoAudioOrVideoElement() {
        String html = "<html><body>"
                + "<p>This is a test document without any audio or video elements.</p>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(0, issues.size());  // No issues should be found
    }
}
