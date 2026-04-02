package com.looksee.audit.informationArchitecture.audits;

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
 * Audits UI components for WCAG 2.1 Section 1.3.6 compliance, ensuring that the purpose
 * of icons, regions, and controls can be programmatically determined.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class IdentifyPurposeAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;

	public IdentifyPurposeAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Audits UI components for programmatic purpose identification per WCAG 2.1 Section 1.3.6.
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
		labels.add("tables");
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
                                                                issues.isEmpty() ? 1 : 0,
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
                                AuditName.IDENTIFY_PURPOSE,
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
     * This method checks for WCAG 2.1 Section 1.3.6 compliance in an HTML document.
     * Specifically, it ensures that the purpose of user interface components, icons, and regions can be programmatically determined.
     * It checks for the presence of relevant attributes like aria-label, aria-labelledby, role, and alt.
     *
     * @param doc The Jsoup Document object representing the HTML to check.
     * @return A list of GenericIssue objects describing any compliance issues found.
     * @throws IllegalArgumentException if the provided document is null.
     */
    public static List<GenericIssue> checkCompliance(Document doc) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        List<GenericIssue> issues = new ArrayList<>();
        
        // Check for images with missing or empty alt attributes
        Elements images = doc.select("img:not([alt]), img[alt='']");
        for (Element img : images) {
            issues.add(new GenericIssue(
                    "Image element is missing a valid alt attribute",
                    "Missing alt attribute",
                    img.cssSelector(),
                    "Add a meaningful alt attribute to the image element to describe its purpose."
            ));
        }

        // Check for buttons or input[type="button"] without aria-label or aria-labelledby
        Elements buttons = doc.select("button:not([aria-label]):not([aria-labelledby]), input[type='button']:not([aria-label]):not([aria-labelledby])");
        for (Element button : buttons) {
            issues.add(new GenericIssue(
                    "Button is missing a valid aria-label or aria-labelledby attribute",
                    "Missing aria-label/aria-labelledby",
                    button.cssSelector(),
                    "Add an aria-label or aria-labelledby attribute to describe the button's purpose."
            ));
        }

        // Check for regions (like divs or sections) without role, aria-label, or aria-labelledby
        Elements regions = doc.select("div[role], section[role], nav[role], header[role], footer[role]");
        for (Element region : regions) {
            if (!region.hasAttr("aria-label") && !region.hasAttr("aria-labelledby")) {
                issues.add(new GenericIssue(
                        "Region element with role attribute is missing aria-label or aria-labelledby",
                        "Missing aria-label/aria-labelledby for region",
                        region.cssSelector(),
                        "Add an aria-label or aria-labelledby attribute to the region to describe its purpose."
                ));
            }
        }

        return issues;
    }
}
