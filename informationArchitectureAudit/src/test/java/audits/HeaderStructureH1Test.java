package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.HeaderStructureAudit;

class HeaderStructureH1Test {

    @Test
    void checkH1HeadersReturnsNullWhenNoHeadersPresent() {
        Document doc = Jsoup.parse("<html><body><p>No heading</p></body></html>");

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertNull(result);
    }

    @Test
    void checkH1HeadersReturnsTrueWhenExactlyOneHeaderPresent() {
        Document doc = Jsoup.parse("<html><body><h1>Title</h1></body></html>");

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertTrue(result);
    }

    @Test
    void checkH1HeadersReturnsFalseWhenMultipleHeadersPresent() {
        Document doc = Jsoup.parse("<html><body><h1>Title</h1><h1>Duplicate</h1></body></html>");

        Boolean result = HeaderStructureAudit.checkH1Headers(doc);

        assertFalse(result);
    }

    @Test
    void checkH1HeadersThrowsForNullDocument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> HeaderStructureAudit.checkH1Headers(null));

        assertEquals("Document must not be null", ex.getMessage());
    }
}
