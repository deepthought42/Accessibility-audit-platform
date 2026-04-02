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
 * Audits HTML form elements for WCAG 2.1 Section 3.3.2 compliance, checking label
 * association, fieldset grouping, and semantic structure.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class FormStructureAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public FormStructureAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 *
	 * Audits form controls on a page for labeling and semantic structure compliance.
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
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Forms";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("forms");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());
        List<Element> forms = jsoup_doc.getElementsByTag("form");
        List<GenericIssue> issues = new ArrayList<>();
        for(Element form: forms){
            issues.addAll(validateForm(form));
        }
        
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
		
		String description = "Form structure and labeling provide the semantic context assistive technologies need.";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
                                AuditSubcategory.NAVIGATION,
                                AuditName.FORM_STRUCTURE,
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
     * Validates a single HTML form element for WCAG 2.1 Section 1.3.1 compliance.
     *
     * @pre {@code form != null}
     * @post returned list is non-null
     * @param form The form element to validate.
     * @return A list of validation messages.
     */
    public static List<GenericIssue> validateForm(Element form) {
        Objects.requireNonNull(form, "form must not be null");
        List<GenericIssue> validationMessages = new ArrayList<>();

        // Select all input, select, and textarea elements within the form
        Elements controls = form.select("input, select, textarea");

        for (Element control : controls) {
            // Check if the control has an associated label
            String id = control.id();
            Element label = form.select("label[for=" + id + "]").first();

            if (label == null) {
                // If no associated label is found, check for alternative attributes like aria-label or aria-labelledby
                String ariaLabel = control.attr("aria-label");
                String ariaLabelledBy = control.attr("aria-labelledby");

                if (ariaLabel.isEmpty() && ariaLabelledBy.isEmpty()) {
                    //validationMessages.add("Form control with id '" + id + "' is missing an associated label, aria-label, or aria-labelledby attribute.");

                    String description = "Form control with id '" + id + "' is missing an associated label, aria-label, or aria-labelledby attribute.";
                    String title = "Form control is missing label";
                    String recommendation = "Add a <label> element and associate it with the input control, or add either the aria-label or aria-labelledby attribute to the input control.";
                    validationMessages.add(new GenericIssue(description, title, control.cssSelector(), recommendation));
                } else {
                    //validationMessages.add("Form control with id '" + id + "' uses aria-label or aria-labelledby attributes.");
                    String description = "Form control with id '" + id + "' has an associated label via aria attributes with text: " + ariaLabel;
                    String title = "Form control has associated label!";
                    String recommendation = "";
                    validationMessages.add(new GenericIssue(description, title, control.cssSelector(), recommendation));
                }
            } else {
                //validationMessages.add("Form control with id '" + id + "' has an associated label with text: " + label.text());
                
                String description = "Form control with id '" + id + "' has an associated label with text: " + label.text();
                String title = "Form control has associated label!";
                String recommendation = "";
                validationMessages.add(new GenericIssue(description, title, control.cssSelector(), recommendation));
            }
        }

        return validationMessages;
    }

    /**
     * Validates that form elements are grouped correctly using fieldset and legend elements.
     *
     * @pre {@code form != null}
     * @post returned list is non-null
     * @param form The form element to validate.
     * @return A list of validation messages.
     */
    public static List<String> validateFieldsetGrouping(Element form) {
        Objects.requireNonNull(form, "form must not be null");
        List<String> validationMessages = new ArrayList<>();

        // Select all fieldset elements within the form
        Elements fieldsets = form.select("fieldset");

        if (fieldsets.isEmpty()) {
            validationMessages.add("No <fieldset> elements found in the form. Consider grouping related controls using <fieldset> and <legend>.");
        } else {
            for (Element fieldset : fieldsets) {
                // Check if the fieldset has a legend
                Element legend = fieldset.select("legend").first();
                if (legend == null) {
                    validationMessages.add("Fieldset without a <legend> found. Ensure each fieldset has a <legend> to describe its purpose.");
                } else {
                    validationMessages.add("Fieldset with <legend>: '" + legend.text() + "' found.");
                }

                // Check if fieldset contains form controls
                Elements controlsInFieldset = fieldset.select("input, select, textarea");
                if (controlsInFieldset.isEmpty()) {
                    validationMessages.add("Fieldset with legend '" + legend.text() + "' does not contain any form controls.");
                }
            }
        }

        // Check for form controls outside of fieldsets
        Elements controlsOutsideFieldset = form.select("> input, > select, > textarea");
        if (!controlsOutsideFieldset.isEmpty()) {
            validationMessages.add("Form controls found outside of fieldsets. Consider grouping these controls within a <fieldset>.");
        }

        return validationMessages;
    }
}
