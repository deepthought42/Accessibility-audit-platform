package com.looksee.audit.informationArchitecture.audits;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

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
 * Audits page elements for WCAG 2.1 Section 1.4.1 compliance, checking that color is not
 * the sole means of conveying information.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class UseOfColorAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
    // List of autocomplete values that help identify input purposes according to WCAG 2.1 section 1.3.5
    private static final List<String> AUTOCOMPLETE_VALUES = Arrays.asList(
        "name", "honorific-prefix", "given-name", "additional-name", "family-name", "honorific-suffix",
        "nickname", "email", "username", "new-password", "current-password", "organization-title",
        "organization", "street-address", "address-line1", "address-line2", "address-line3", 
        "address-level1", "address-level2", "address-level3", "address-level4", "country", 
        "country-name", "postal-code", "cc-name", "cc-given-name", "cc-additional-name", 
        "cc-family-name", "cc-number", "cc-exp", "cc-exp-month", "cc-exp-year", "cc-csc", 
        "cc-type", "transaction-currency", "transaction-amount", "language", "bday", 
        "bday-day", "bday-month", "bday-year", "sex", "tel", "tel-country-code", "tel-national", 
        "tel-area-code", "tel-local", "tel-local-prefix", "tel-local-suffix", "tel-extension", 
        "impp", "url", "photo"
    );

     // Pattern for basic fuzzy matching of input names
     private static final Pattern NAME_PATTERN = Pattern.compile(".*(name|email|address|phone|credit|dob|gender|username|city).*", Pattern.CASE_INSENSITIVE);

	public UseOfColorAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 *
	 * Checks elements for color-only information conveyance per WCAG 2.1 Section 1.4.1.
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
								 AuditName.USE_OF_COLOR,
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
     * Checks the document for WCAG 2.1 Section 1.4.1 (Use of Color) compliance.
     *
     * @pre {@code doc != null}
     * @post returned list is non-null
     * @param doc the parsed HTML document
     * @return list of color-only information issues found
     */
    public static List<GenericIssue> checkCompliance(Document doc) {
        Objects.requireNonNull(doc, "Document must not be null");
        List<GenericIssue> issues = new ArrayList<>();

        // Check for elements that use inline styles or attributes to convey information by color
        Elements elements = doc.select("*[style], *[bgcolor]");

        for (Element element : elements) {
            String style = element.attr("style");

            // Check for use of color in the inline style or the bgcolor attribute
            if (style.contains("color") || style.contains("background-color") || element.hasAttr("bgcolor")) {
                // Check if there's any accompanying text or visual indicator
                if (!hasTextualIndicator(element)) {
                    issues.add(new GenericIssue(
                            "Element relies on color alone to convey information.",
                            "Use of Color Violation",
                            element.cssSelector(),
                            "Add textual indicators or non-color visual indicators to ensure the information is accessible."
                    ));
                }
            }
        }

        return issues;
    }

    // Method to check if the element or its children contain textual indicators
    private static boolean hasTextualIndicator(Element element) {
        // Check if the element or any of its children contain text
        if (!element.ownText().trim().isEmpty()) {
            return true;
        }

        // Check if there are any children with text content
        for (Element child : element.children()) {
            if (!child.ownText().trim().isEmpty()) {
                return true;
            }
        }

        // Check if there's an aria-label or other accessibility attribute that conveys meaning
        if (element.hasAttr("aria-label") || element.hasAttr("aria-labelledby") || element.hasAttr("alt")) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        String html = "<html><body>"
                + "<div style='color: red;'>Required</div>"
                + "<div style='background-color: #00FF00;'>Success</div>"
                + "<span bgcolor='#FF0000'>Alert</span>"
                + "<button style='color: green;'>Submit</button>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = checkCompliance(doc);

        for (GenericIssue issue : issues) {
            System.out.println(issue);
        }
    }
}
