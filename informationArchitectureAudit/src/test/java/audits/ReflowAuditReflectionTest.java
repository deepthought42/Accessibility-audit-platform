package audits;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.ReflowAudit;
import com.looksee.models.ElementState;
import com.looksee.models.audit.messages.UXIssueMessage;

class ReflowAuditReflectionTest {

    @Test
    void isFixedWidthReturnsTrueForPixelsAndPercent() throws Exception {
        ReflowAudit audit = new ReflowAudit();
        Method method = ReflowAudit.class.getDeclaredMethod("isFixedWidth", String.class);
        method.setAccessible(true);

        boolean pxResult = (boolean) method.invoke(audit, "320px");
        boolean percentResult = (boolean) method.invoke(audit, "80%");

        assertTrue(pxResult);
        assertTrue(percentResult);
    }

    @Test
    void isFixedWidthReturnsFalseForResponsiveWidthValues() throws Exception {
        ReflowAudit audit = new ReflowAudit();
        Method method = ReflowAudit.class.getDeclaredMethod("isFixedWidth", String.class);
        method.setAccessible(true);

        boolean emResult = (boolean) method.invoke(audit, "40em");
        boolean autoResult = (boolean) method.invoke(audit, "auto");

        assertFalse(emResult);
        assertFalse(autoResult);
    }

    @Test
    void checkElementForReflowComplianceReturnsIssueWhenWidthIsFixedAndOverflowNotVisible() throws Exception {
        ReflowAudit audit = new ReflowAudit();
        Method method = ReflowAudit.class.getDeclaredMethod("checkElementForReflowCompliance", ElementState.class);
        method.setAccessible(true);

        ElementState element = mock(ElementState.class);
        when(element.getRenderedCssValues()).thenReturn(Map.of("width", "500px", "overflow", "hidden"));
        when(element.getName()).thenReturn("div");
        when(element.getCssSelector()).thenReturn("body > div");

        UXIssueMessage issue = (UXIssueMessage) method.invoke(audit, element);

        assertTrue(issue.getTitle().contains("doesn't adjust to fit the viewport"));
        assertTrue(issue.getDescription().contains("doesn't properly adjust"));
    }

    @Test
    void checkElementForReflowComplianceHandlesSafeStyles() throws Exception {
        ReflowAudit audit = new ReflowAudit();
        Method method = ReflowAudit.class.getDeclaredMethod("checkElementForReflowCompliance", ElementState.class);
        method.setAccessible(true);

        ElementState element = mock(ElementState.class);
        when(element.getRenderedCssValues()).thenReturn(Map.of("width", "auto", "overflow", "visible"));
        when(element.getName()).thenReturn("main");

        UXIssueMessage issue = (UXIssueMessage) method.invoke(audit, element);

        assertTrue(issue.getTitle().contains("adjusts to fit the viewport"));
    }

}
