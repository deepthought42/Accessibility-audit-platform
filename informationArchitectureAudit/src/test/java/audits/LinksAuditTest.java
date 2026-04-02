package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.LinksAudit;

public class LinksAuditTest {

    @SuppressWarnings("unchecked")
    private List<String> getBadLinkTextList(LinksAudit audit) throws Exception {
        Field field = LinksAudit.class.getDeclaredField("bad_link_text_list");
        field.setAccessible(true);
        return (List<String>) field.get(audit);
    }

    @Test
    void testConstructor_initializesBadLinkTextList() throws Exception {
        LinksAudit audit = new LinksAudit();
        List<String> badLinkTextList = getBadLinkTextList(audit);

        assertNotNull(badLinkTextList);
        assertTrue(badLinkTextList.contains("click here"));
        assertTrue(badLinkTextList.contains("here"));
        assertTrue(badLinkTextList.contains("more"));
        assertTrue(badLinkTextList.contains("read more"));
        assertTrue(badLinkTextList.contains("learn more"));
        assertTrue(badLinkTextList.contains("info"));
        assertEquals(6, badLinkTextList.size());
    }

    @Test
    void testBadLinkTextList_doesNotContainGoodText() throws Exception {
        LinksAudit audit = new LinksAudit();
        List<String> badLinkTextList = getBadLinkTextList(audit);

        assertFalse(badLinkTextList.contains("View our pricing plans"));
        assertFalse(badLinkTextList.contains("Contact support"));
        assertFalse(badLinkTextList.contains("Download the report"));
    }

    @Test
    void testBadLinkTextList_caseMatching() throws Exception {
        LinksAudit audit = new LinksAudit();
        List<String> badLinkTextList = getBadLinkTextList(audit);

        assertTrue(badLinkTextList.contains("click here"));
        assertFalse(badLinkTextList.contains("Click Here"));
        assertFalse(badLinkTextList.contains("HERE"));
    }
}
