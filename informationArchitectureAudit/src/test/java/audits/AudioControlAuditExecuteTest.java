package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.AudioControlAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.GenericIssue;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class AudioControlAuditExecuteTest {

    private AudioControlAudit audioControlAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        audioControlAudit = new AudioControlAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = AudioControlAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audioControlAudit, mockAuditService);

        Field f2 = AudioControlAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(audioControlAudit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    // ---- Static checkCompliance tests ----

    @Test
    void testCheckCompliance_noAudioVideo() {
        String html = "<html><body><p>Simple page with no media elements.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Should find no issues when no audio/video elements exist");
    }

    @Test
    void testCheckCompliance_audioWithAutoplayNoControls() {
        String html = "<html><body>"
                + "<audio autoplay><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size(), "Should find one issue for autoplay audio without controls");
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
        assertTrue(issues.get(0).getDescription().contains("Autoplaying audio or video found without user controls"));
    }

    @Test
    void testCheckCompliance_audioWithAutoplayAndControls() {
        String html = "<html><body>"
                + "<audio autoplay controls><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Should find no issues when autoplay audio has controls attribute");
    }

    @Test
    void testCheckCompliance_videoWithAutoplay() {
        String html = "<html><body>"
                + "<video autoplay><source src='video.mp4' type='video/mp4'></video>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size(), "Should find one issue for autoplay video without controls");
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
    }

    @Test
    void testCheckCompliance_iframeWithAutoplay() {
        String html = "<html><body>"
                + "<iframe src='https://example.com/video?autoplay=1'></iframe>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertEquals(1, issues.size(), "Should find one issue for iframe with autoplay parameter");
        assertEquals("Audio Control Violation", issues.get(0).getTitle());
        assertTrue(issues.get(0).getDescription().contains("Embedded content with autoplaying audio or video found"));
    }

    @Test
    void testCheckCompliance_audioWithMuted() {
        String html = "<html><body>"
                + "<audio autoplay muted><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = AudioControlAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Should find no issues when autoplay audio is muted");
    }

    // ---- Execute tests ----

    @Test
    void testExecute_noIssues() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><p>Clean page with no media.</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = audioControlAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Should have no issue messages for a clean page");
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_withAutoplayAudio() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body>"
                + "<audio autoplay><source src='audio.mp3' type='audio/mpeg'></audio>"
                + "</body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = audioControlAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issue messages for autoplay audio");
        assertEquals(1, result.getMessages().size());
        verify(mockElementStateService, times(1)).findByPageAndCssSelector(anyLong(), anyString());
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }
}
