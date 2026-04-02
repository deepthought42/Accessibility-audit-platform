package audits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.looksee.audit.informationArchitecture.models.KeyboardAccessibleAudit;
import com.looksee.models.ElementState;

public class KeyboardAccessibleTest {

    @Test
    void testElementWithTabindexIsAccessible() {
        ElementState element = Mockito.mock(ElementState.class);
        Mockito.when(element.getAttribute("tabindex")).thenReturn("0");
        Mockito.when(element.getAttribute("role")).thenReturn(null);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(List.of(element));

        assertTrue(result.isEmpty(), "Element with tabindex should be keyboard accessible");
    }

    @Test
    void testElementWithButtonRoleIsAccessible() {
        ElementState element = Mockito.mock(ElementState.class);
        Mockito.when(element.getAttribute("tabindex")).thenReturn(null);
        Mockito.when(element.getAttribute("role")).thenReturn("button");

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(List.of(element));

        assertTrue(result.isEmpty(), "Element with role should be keyboard accessible");
    }

    @Test
    void testElementWithNoAttributesIsNotAccessible() {
        ElementState element = Mockito.mock(ElementState.class);
        Mockito.when(element.getAttribute("tabindex")).thenReturn(null);
        Mockito.when(element.getAttribute("role")).thenReturn(null);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(List.of(element));

        assertEquals(1, result.size(), "Element with no keyboard semantics should be reported");
    }

    @Test
    void testElementWithMixedAccessOnlyFlagsInaccessibleElements() {
        ElementState accessible = Mockito.mock(ElementState.class);
        Mockito.when(accessible.getAttribute("tabindex")).thenReturn("0");
        Mockito.when(accessible.getAttribute("role")).thenReturn(null);

        ElementState inaccessible = Mockito.mock(ElementState.class);
        Mockito.when(inaccessible.getAttribute("tabindex")).thenReturn("");
        Mockito.when(inaccessible.getAttribute("role")).thenReturn("");

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(List.of(accessible, inaccessible));

        assertEquals(1, result.size(), "Only inaccessible elements should be returned");
        assertEquals(inaccessible, result.get(0));
    }
}
