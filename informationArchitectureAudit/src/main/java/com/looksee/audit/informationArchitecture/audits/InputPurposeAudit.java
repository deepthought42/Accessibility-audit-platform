package com.looksee.audit.informationArchitecture.audits;

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
 * Audits input fields for WCAG 2.1 Section 1.3.5 compliance, verifying that autocomplete
 * attributes and ARIA labels correctly identify the purpose of each input.
 *
 * <p><strong>Class invariant:</strong> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class InputPurposeAudit implements IExecutablePageStateAudit {

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

	public InputPurposeAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 *
	 * Evaluates input elements on the page for WCAG 2.1 Section 1.3.5 compliance by checking
	 * autocomplete attributes and ARIA labels.
	 *
	 * @pre {@code page_state != null} -- the page state to audit must be provided
	 * @pre {@code audit_record != null} -- the audit record context must be provided
	 * @post the returned {@code Audit} is non-null and has been persisted via {@code auditService}
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "Precondition failed: page_state must not be null");
		Objects.requireNonNull(audit_record, "Precondition failed: audit_record must not be null");

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
                                AuditName.INPUT_PURPOSE,
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
     * Checks the given HTML document for compliance with WCAG 2.1 Section 1.3.5, which focuses on ensuring that 
     * form controls are labeled and that their purposes are clear to assistive technologies.
     * 
     * This method performs the following checks:
     * 
     * 1. **Autocomplete Attribute**: Verifies that the `autocomplete` attribute on `<input>` elements is present and 
     *    contains a value that is listed in the set of accepted values according to WCAG 2.1 Section 1.3.5. If the 
     *    `autocomplete` attribute is missing or contains a value that is not recognized, an issue is reported.
     * 
     * 2. **ARIA Label**: Ensures that the `aria-label` attribute is present on `<input>` elements and provides a meaningful 
     *    description that corresponds to the input's purpose. If the `aria-label` attribute is missing or does not match 
     *    expected naming conventions (using fuzzy checks), an issue is reported.
     * 
     * **Prerequisites for Method Execution:**
     * - The HTML document (`doc`) must not be null. If it is null, an `IllegalArgumentException` will be thrown.
     * 
     * **Returns:**
     * - A `List<GenericIssue>` that contains all identified issues related to the autocomplete attribute and ARIA labels 
     *   for `<input>` elements in the document. Each `GenericIssue` provides a description, title, CSS selector, and 
     *   recommendation for addressing the issue.
     * 
     * **Postconditions:**
     * - The returned list of issues should not be null and should contain issues found during the compliance check.
     * 
     * @pre {@code doc != null} -- the HTML document must not be null
     * @post the returned list is never null
     * @param doc The HTML document to be checked. Must be a valid non-null {@code Document} object.
     * @return A list of {@code GenericIssue} objects representing any non-compliance issues found. The list is never null.
     */
    public static List<GenericIssue> checkCompliance(Document doc) {
        List<GenericIssue> issues = new ArrayList<>();

        // Preconditions: Document should not be null
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        // Select all input elements within the document
        Elements inputElements = doc.select("input");

        // Iterate over each input element to check for compliance
        for (Element input : inputElements) {
            String autocomplete = input.attr("autocomplete");
            String name = input.attr("name");
            String ariaLabel = input.attr("aria-label");

            // Check if the autocomplete attribute is missing or has an incorrect value
            if (autocomplete.isEmpty() || !AUTOCOMPLETE_VALUES.contains(autocomplete)) {
                String description = "Input element autocomplete attribute is missing or incorrect.";
                String title = "Non-compliant Autocomplete Attribute";
                String cssSelector = input.cssSelector();
                String recommendation = "Ensure that the autocomplete attribute is present and has a valid value. " +
                                        "Refer to WCAG 2.1 section 1.3.5 for the list of acceptable values.";

                // Add the issue to the list of issues
                issues.add(new GenericIssue(description, title, cssSelector, recommendation));
            }
            else{
                String description = "Input element has an autocomplete attribute.";
                String title = "Compliant autocomplete attribute";
                String cssSelector = input.cssSelector();
                String recommendation = "";

                // Add the issue to the list of issues
                issues.add(new GenericIssue(description, title, cssSelector, recommendation));
            }

            // Check if aria-label is missing or does not match the input's purpose
            if (ariaLabel.isEmpty() || !isAriaLabelMeaningful(ariaLabel, name)) {
                String description = "Input element's ARIA label is missing or does not match the purpose of the input.";
                String title = "Non-compliant ARIA Label";
                String cssSelector = input.cssSelector();
                String recommendation = "Ensure that the ARIA label is present and provides meaningful information about the input's purpose.";

                // Add the issue to the list of issues
                issues.add(new GenericIssue(description, title, cssSelector, recommendation));
            }
            else{
                String description = "Input element's ARIA label matches the purpose of the input.";
                String title = "Compliant ARIA label";
                String cssSelector = input.cssSelector();
                String recommendation = "";

                // Add the issue to the list of issues
                issues.add(new GenericIssue(description, title, cssSelector, recommendation));
            }
        }

        return issues;
    }

    /**
     * Checks if the ARIA label is meaningful by comparing it to common input names.
     *
     * @pre {@code ariaLabel != null} -- the ARIA label must not be null
     * @pre {@code name != null} -- the name attribute must not be null
     * @post returns {@code true} if the ARIA label or name matches common patterns; {@code false} otherwise
     * @param ariaLabel The ARIA label of the input element.
     * @param name The name attribute of the input element.
     * @return True if the ARIA label or name matches common patterns; false otherwise.
     */
    private static boolean isAriaLabelMeaningful(String ariaLabel, String name) {
        // Preconditions: ariaLabel and name should not be null
        if (ariaLabel == null || name == null) {
            throw new IllegalArgumentException("ARIA label and name cannot be null");
        }

        // Simple fuzzy check to determine if the aria-label matches the input name's expected purpose
        return NAME_PATTERN.matcher(ariaLabel).find() || NAME_PATTERN.matcher(name).find();
    }
}
