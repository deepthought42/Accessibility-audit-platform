package com.looksee.contentAudit.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

public class FigureAltTextAuditTest {

	private FigureAltTextAudit audit;
	private AuditService auditService;
	private UXIssueMessageService issueMessageService;

	@Before
	public void setUp() throws Exception {
		audit = new FigureAltTextAudit();
		auditService = mock(AuditService.class);
		issueMessageService = mock(UXIssueMessageService.class);

		Field auditServiceField = FigureAltTextAudit.class.getDeclaredField("audit_service");
		auditServiceField.setAccessible(true);
		auditServiceField.set(audit, auditService);

		Field issueServiceField = FigureAltTextAudit.class.getDeclaredField("issue_message_service");
		issueServiceField.setAccessible(true);
		issueServiceField.set(audit, issueMessageService);

		when(issueMessageService.save(any(UXIssueMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(auditService.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	public void executeWithNoFigureElementsReturnsZeroScoreAudit() {
		PageState pageState = mock(PageState.class);
		List<ElementState> elements = new ArrayList<>();
		ElementState divElement = mock(ElementState.class);
		when(divElement.getName()).thenReturn("div");
		elements.add(divElement);
		when(pageState.getElements()).thenReturn(elements);
		when(pageState.getUrl()).thenReturn("http://example.com");

		Audit result = audit.execute(pageState, mock(AuditRecord.class), null);

		assertNotNull(result);
		assertEquals(AuditCategory.CONTENT, result.getCategory());
		assertEquals(AuditSubcategory.IMAGERY, result.getSubcategory());
		assertEquals(AuditName.ALT_TEXT, result.getName());
		assertEquals(0, result.getPoints());
		assertEquals(0, result.getTotalPossiblePoints());
	}

	@Test
	public void executeWithFigureHavingFigcaptionScoresFullPoints() {
		PageState pageState = mock(PageState.class);
		List<ElementState> elements = new ArrayList<>();

		ElementState figureElement = mock(ElementState.class);
		when(figureElement.getName()).thenReturn("figure");
		when(figureElement.getAllText()).thenReturn("<figure><img src=\"img.jpg\"><figcaption>A description</figcaption></figure>");
		when(figureElement.getId()).thenReturn(1L);
		elements.add(figureElement);

		when(pageState.getElements()).thenReturn(elements);
		when(pageState.getUrl()).thenReturn("http://example.com");

		Audit result = audit.execute(pageState, mock(AuditRecord.class), null);

		assertNotNull(result);
		assertEquals(1, result.getPoints());
		assertEquals(1, result.getTotalPossiblePoints());
	}

	@Test
	public void executeWithFigureMissingFigcaptionScoresZero() {
		PageState pageState = mock(PageState.class);
		List<ElementState> elements = new ArrayList<>();

		ElementState figureElement = mock(ElementState.class);
		when(figureElement.getName()).thenReturn("figure");
		when(figureElement.getAllText()).thenReturn("<figure><img src=\"img.jpg\"></figure>");
		when(figureElement.getId()).thenReturn(1L);
		elements.add(figureElement);

		when(pageState.getElements()).thenReturn(elements);
		when(pageState.getUrl()).thenReturn("http://example.com");

		Audit result = audit.execute(pageState, mock(AuditRecord.class), null);

		assertNotNull(result);
		assertEquals(0, result.getPoints());
		assertEquals(1, result.getTotalPossiblePoints());
	}

	@Test
	public void executeWithFigureHavingEmptyFigcaptionScoresZero() {
		PageState pageState = mock(PageState.class);
		List<ElementState> elements = new ArrayList<>();

		ElementState figureElement = mock(ElementState.class);
		when(figureElement.getName()).thenReturn("figure");
		when(figureElement.getAllText()).thenReturn("<figure><img src=\"img.jpg\"><figcaption></figcaption></figure>");
		when(figureElement.getId()).thenReturn(1L);
		elements.add(figureElement);

		when(pageState.getElements()).thenReturn(elements);
		when(pageState.getUrl()).thenReturn("http://example.com");

		Audit result = audit.execute(pageState, mock(AuditRecord.class), null);

		assertNotNull(result);
		assertEquals(0, result.getPoints());
		assertEquals(1, result.getTotalPossiblePoints());
	}

	@Test
	public void executeFiltersOnlyFigureElements() {
		PageState pageState = mock(PageState.class);
		List<ElementState> elements = new ArrayList<>();

		ElementState figureElement = mock(ElementState.class);
		when(figureElement.getName()).thenReturn("figure");
		when(figureElement.getAllText()).thenReturn("<figure><figcaption>Caption</figcaption></figure>");
		when(figureElement.getId()).thenReturn(1L);
		elements.add(figureElement);

		ElementState imgElement = mock(ElementState.class);
		when(imgElement.getName()).thenReturn("img");
		elements.add(imgElement);

		when(pageState.getElements()).thenReturn(elements);
		when(pageState.getUrl()).thenReturn("http://example.com");

		Audit result = audit.execute(pageState, mock(AuditRecord.class), null);

		assertEquals(1, result.getPoints());
		assertEquals(1, result.getTotalPossiblePoints());
	}
}
