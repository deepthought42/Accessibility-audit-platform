package com.looksee.audit.informationArchitecture.models;

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
 * Audits list structure for WCAG 2.1 Section 1.3.1 compliance, checking that ul/ol elements
 * contain only li children.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class ListStructureAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public ListStructureAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
		
		bad_link_text_list = new ArrayList<>();
		bad_link_text_list.add("click here");
		bad_link_text_list.add("here");
		bad_link_text_list.add("more");
		bad_link_text_list.add("read more");
		bad_link_text_list.add("learn more");
		bad_link_text_list.add("info");
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores list structure on a page based on if the list is structured correctly
     * 	based on the WCAG 2.1 Section 1.3.1 requirements
	 *   
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		Objects.requireNonNull(audit_record, "audit_record must not be null");

		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Structure";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("headers");
		labels.add("wcag");
		
		Document jsoup_doc = Jsoup.parse(page_state.getSrc());

        for(String cssSelector: checkListCompliance(jsoup_doc)){
            ElementState header_elem = elementStateService.findByPageAndCssSelector(page_state.getId(), cssSelector);
            String issue_description = "Having headers in hierarchical order is crucial for accessibility and WCAG 2.1 compliance because it provides a clear and logical structure to the content. This hierarchy helps users, especially those using assistive technologies like screen readers, to easily navigate the webpage and understand the relationship between different sections. Properly ordered headers guide users through the content, improving their experience and ensuring the website is accessible to all.\n";
            String recommendation = "Reconfigure document so that headers are in hierarchical order. When headers are not in hierarchical order, it makes content difficult to understand for people that require assistive technology";
            String title = "Headers are not in hierarchical order.";
            issue_messages.add(new ElementStateIssueMessage(
                Priority.MEDIUM,
                issue_description,
                recommendation,
                header_elem,
                AuditCategory.ACCESSIBILITY,
                labels,
                ada_compliance,
                title,
                0,
                1));
        }

		String why_it_matters = "A well-structured header hierarchy is like a road map for your content—it helps screen readers and assistive technologies navigate the page, making it easier for everyone to understand the content flow. When headers are in the correct order, users can skim and comprehend information more efficiently, which is key to meeting WCAG 2.1 Section 1.3.1 requirements.";
		
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
                                AuditName.LINKS,
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
     * Checks if the HTML document complies with WCAG 2.1 Section 1.3.1 regarding lists and returns
     * a list of CSS selector strings for all <ul> and <ol> elements that contain non-<li> children.
     *
     * @param doc The JSoup Document object representing the HTML content.
     *            Precondition: doc is not null and must be a well-formed HTML document.
     *
     * @return A list of CSS selector strings for all <ul> and <ol> elements that contain non-<li> children.
     *         Postcondition: The list contains CSS selectors for all non-compliant <ul> and <ol> elements,
     *         or is empty if all lists are compliant.
     */
    public static List<String> checkListCompliance(Document doc) {
        Objects.requireNonNull(doc, "Precondition failed: document must not be null");

        List<String> nonCompliantSelectors = new ArrayList<>();

        // Select all <ul> and <ol> elements in the document
        Elements ulElements = doc.select("ul");
        Elements olElements = doc.select("ol");

        // Check if all <ul> and <ol> elements contain only <li> elements as children
        for (Element ul : ulElements) {
            if (!areChildrenListItems(ul)) {
                nonCompliantSelectors.add(ul.cssSelector());
            }
        }

        for (Element ol : olElements) {
            if (!areChildrenListItems(ol)) {
                nonCompliantSelectors.add(ol.cssSelector());
            }
        }

        return nonCompliantSelectors;
    }

    /**
     * Verifies if all direct children of a given list element are <li> elements.
     * 
     * @param listElement The JSoup Element object representing a <ul> or <ol> element.
     *                    Precondition: listElement is not null and must represent a <ul> or <ol> element.
     * 
     * @return true if all children of listElement are <li> elements, false otherwise.
     *         Postcondition: The return value is true if and only if all direct children of listElement are <li> elements.
     */
    public static boolean areChildrenListItems(Element listElement) {
        Objects.requireNonNull(listElement, "Precondition failed: listElement must not be null");
        if (!listElement.tagName().equals("ul") && !listElement.tagName().equals("ol")) {
            throw new IllegalArgumentException("Precondition failed: element must be a <ul> or <ol>");
        }

        // Get the direct children of the list element
        Elements children = listElement.children();

        // Ensure each child is an <li> element
        for (Element child : children) {
            if (!child.tagName().equals("li")) {
                return false;
            }
        }

        return true;
    }
}
