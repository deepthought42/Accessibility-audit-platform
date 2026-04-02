package com.looksee.audit.informationArchitecture.audits;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.GenericIssue;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

/**
 * Audits visual presentation for WCAG 2.1 Section 1.4.8 compliance, checking foreground/background
 * colors, font sizing, text justification, line height, and paragraph spacing.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class VisualPresentationAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public VisualPresentationAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Checks visual presentation properties against WCAG 2.1 Section 1.4.8 requirements.
	 *
	 * @pre {@code page_state != null}
	 * @pre {@code audit_record != null}
	 * @post returned {@code Audit} is non-null and persisted
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		Objects.requireNonNull(audit_record, "audit_record must not be null");

		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Tables";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        List<GenericIssue> issues = checkCompliance(jsoup_doc);

        for(GenericIssue issue: issues){
            ElementState element_state = elementStateService.findByPageAndCssSelector(page_state.getId(), issue.getCssSelector());
            UXIssueMessage issue_msg = new ElementStateIssueMessage(Priority.HIGH,
                                                                issue.getDescription(),
                                                                issue.getRecommendation(),
                                                                element_state,
                                                                AuditCategory.ACCESSIBILITY,
                                                                labels,
                                                                ada_compliance,
                                                                issue.getTitle(),
                                                                0,
                                                                1);
            issue_messages.add(issue_msg);
        }

		String why_it_matters = "Grouping form controls within a <fieldset> element is important for accessibility because it provides a clear, semantic structure that enhances the understanding of the form's organization, especially for users with disabilities. The <fieldset> element, often paired with a <legend>, helps screen readers and other assistive technologies to convey related groups of controls as a single, coherent unit, ensuring that users can navigate and comprehend the form's layout more effectively. This practice aligns with WCAG 2.1 guidelines, supporting a more inclusive and accessible web experience.";
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		String description = "Making sure your links are setup correctly is incredibly important";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
                                AuditSubcategory.NAVIGATION,
                                AuditName.VISUAL_PRESENTATION,
                                points_earned,
                                issue_messages,
                                AuditLevel.PAGE,
                                max_points,
                                page_state.getUrl(),
                                why_it_matters,
                                description,
                                true);

		Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
		return auditService.save(audit);
	}

    
    /**
     * Checks the given HTML document for compliance with WCAG 2.1 Section 1.4.8 Visual Presentation requirements.
     *
     * @pre {@code document != null}
     * @post returned list is non-null
     * @param document The parsed HTML document to check.
     * @return A list of GenericIssue objects that detail any compliance issues found.
     */
    public List<GenericIssue> checkCompliance(Document document) {
        Objects.requireNonNull(document, "Document must not be null");
        List<GenericIssue> issues = new ArrayList<>();

        // Check for the requirements specified in WCAG 2.1 Section 1.4.8

        // 1. Text should be presented with a mechanism for the user to choose foreground and background colors.
        Elements elements = document.select("*");
        for (Element element : elements) {
            if (element.hasAttr("style")) {
                String style = element.attr("style").toLowerCase();
                if (style.contains("color:") && style.contains("background-color:")) {
                    issues.add(new GenericIssue(
                            "Foreground and background colors are hard-coded.",
                            "Foreground and Background Color Issue",
                            element.cssSelector(),
                            "Allow users to choose foreground and background colors."
                    ));
                }
                else{
                    issues.add(new GenericIssue(
                        "Foreground and background colors are NOT hard-coded.",
                        "Foreground and Background Color is Accessible",
                        element.cssSelector(),
                        ""
                ));
                }
            }
        }

        // 2. Text should be resizable up to 200% without assistive technology and without loss of content or functionality.
        Elements fontElements = document.select("[style*=font-size]");
        for (Element element : fontElements) {
            String fontSize = element.attr("style").toLowerCase();
            if (fontSize.contains("font-size") && !fontSize.contains("em") && !fontSize.contains("rem") && !fontSize.contains("%")) {
                issues.add(new GenericIssue(
                        "Font size is not defined in relative units (em, rem, or %).",
                        "Font Size Issue",
                        element.cssSelector(),
                        "Use relative units (em, rem, or %) for font sizes to allow text resizing."
                ));
            }
            else {
                issues.add(new GenericIssue(
                        "Font size is defined in relative units (em, rem, or %).",
                        "Font Size is Accessible",
                        element.cssSelector(),
                        ""
                ));
            }
        }

        // 3. Text should have a minimum contrast ratio of 7:1.
        // This check requires a contrast calculation, which isn't directly supported by JSoup. Implementing a contrast check would require additional logic.

        // 4. No justification of text (left-aligned text only).
        Elements justifiedText = document.select("[style*=text-align: justify]");
        for (Element element : justifiedText) {
            issues.add(new GenericIssue(
                    "Text is justified, which may cause readability issues.",
                    "Text Justification Issue",
                    element.cssSelector(),
                    "Ensure that text is left-aligned instead of justified."
            ));
        }

        // 5. Line spacing (leading) is at least 1.5 times the font size, and paragraph spacing is at least 1.5 times the line spacing.
        Elements lineHeightElements = document.select("[style*=line-height]");
        for (Element element : lineHeightElements) {
            String lineHeight = element.attr("style").toLowerCase();
            if (lineHeight.contains("line-height")) {
                String value = lineHeight.substring(lineHeight.indexOf("line-height:") + 12).trim();
                if (!value.endsWith("em") || Double.parseDouble(value.replace("em", "")) < 1.5) {
                    issues.add(new GenericIssue(
                            "Line height is less than 1.5 times the font size.",
                            "Line Height Issue",
                            element.cssSelector(),
                            "Ensure that line spacing (leading) is at least 1.5 times the font size."
                    ));
                }
                else{
                    issues.add(new GenericIssue(
                        "Line height is at least 1.5 times the font size.",
                        "Line Height is accessible",
                        element.cssSelector(),
                        ""
                ));
                }
            }
        }

        // 6. Paragraph spacing should be at least 1.5 times the line spacing.
        Elements marginElements = document.select("[style*=margin]");
        for (Element element : marginElements) {
            String marginStyle = element.attr("style").toLowerCase();
            if (marginStyle.contains("margin")) {
                String[] margins = marginStyle.substring(marginStyle.indexOf("margin:") + 7).trim().split(" ");
                if (margins.length >= 2 && !margins[0].equals(margins[1])) {
                    issues.add(new GenericIssue(
                            "Paragraph spacing is not consistent with line spacing.",
                            "Paragraph Spacing Issue",
                            element.cssSelector(),
                            "Ensure that paragraph spacing is at least 1.5 times the line spacing."
                    ));
                }
                else{
                    issues.add(new GenericIssue(
                            "Paragraph spacing is consistent with line spacing.",
                            "Paragraph Spacing is accessible",
                            element.cssSelector(),
                            ""
                    ));
                }
            }
        }

        return issues;
    }
}
