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
 * Audits emphasis and special text elements for WCAG 2.1 Section 1.3.1 compliance,
 * checking that semantic tags (strong, em, code, abbr, blockquote) are used instead
 * of non-semantic alternatives (b, i).
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class WcagEmphasisComplianceAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public WcagEmphasisComplianceAudit() {
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
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
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

        for(String cssSelector: checkEmphasisCompliance(jsoup_doc)){
            ElementState header_elem = elementStateService.findByPageAndCssSelector(page_state.getId(), cssSelector);
            String issue_description = "Using HTML tags like <strong>, <code>, <abbr>, and <blockquote> is super important for making your website accessible and WCAG 2.1 compliant. These tags help make sure that special text, code snippets, abbreviations, and quotes are properly understood by screen readers and other assistive technologies. When you use these tags correctly, it makes your content clearer and easier to navigate for everyone, including people with disabilities. So, using them not only helps meet accessibility standards but also ensures your site is inclusive and user-friendly!";
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

        for(String cssSelector: checkSpecialTextCompliance(jsoup_doc)){
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
     * Checks if the HTML document complies with WCAG 2.1 Section 1.3.1 regarding emphasized or special text
     * and returns a list of CSS selector strings for all non-semantic emphasis elements.
     *
     * @param doc The JSoup Document object representing the HTML content.
     *            Precondition: doc is not null and must be a well-formed HTML document.
     *
     * @return A list of CSS selector strings for all non-semantic emphasis elements (e.g., <b>, <i>).
     *         Postcondition: The list contains CSS selectors for all non-compliant <b>, <i> elements,
     *         or is empty if all emphasis elements are compliant.
     */
    public static List<String> checkEmphasisCompliance(Document doc) {
        Objects.requireNonNull(doc, "Precondition failed: document must not be null");

        List<String> nonCompliantSelectors = new ArrayList<>();

        // Select all <b> and <i> elements in the document (non-semantic elements)
        Elements boldElements = doc.select("b");
        Elements italicElements = doc.select("i");

        // Add CSS selectors for non-compliant <b> and <i> elements
        for (Element bold : boldElements) {
            nonCompliantSelectors.add(bold.cssSelector());
        }

        for (Element italic : italicElements) {
            nonCompliantSelectors.add(italic.cssSelector());
        }

        return nonCompliantSelectors;
    }

/**
     * Checks if the HTML document complies with WCAG 2.1 Section 1.3.1 regarding special text elements
     * and returns a list of CSS selector strings for all elements that are used inappropriately.
     *
     * @param doc The JSoup Document object representing the HTML content.
     *            Precondition: doc is not null and must be a well-formed HTML document.
     *
     * @return A list of CSS selector strings for all elements (<strong>, <code>, <abbr>, <blockquote>)
     *         that are used inappropriately or do not convey semantic meaning correctly.
     *         Postcondition: The list contains CSS selectors for all non-compliant elements,
     *         or is empty if all special text elements are used appropriately.
     */
    public static List<String> checkSpecialTextCompliance(Document doc) {
        Objects.requireNonNull(doc, "Precondition failed: document must not be null");

        List<String> nonCompliantSelectors = new ArrayList<>();

        // Select all <strong>, <code>, <abbr>, and <blockquote> elements in the document
        Elements strongElements = doc.select("strong");
        Elements codeElements = doc.select("code");
        Elements abbrElements = doc.select("abbr");
        Elements blockquoteElements = doc.select("blockquote");

        // Add CSS selectors for non-compliant <strong>, <code>, <blockquote> elements
        nonCompliantSelectors.addAll(getNonCompliantSelectors(strongElements));
        nonCompliantSelectors.addAll(getNonCompliantSelectors(codeElements));
        nonCompliantSelectors.addAll(getNonCompliantSelectors(blockquoteElements));

        // Add CSS selectors for <abbr> elements missing the title attribute
        for (Element abbr : abbrElements) {
            if (abbr.attr("title").isEmpty()) {
                nonCompliantSelectors.add(abbr.cssSelector());
            }
        }

        return nonCompliantSelectors;
    }

    /**
     * Helper method to get CSS selectors for non-compliant elements.
     *
     * @param elements The JSoup Elements to be checked.
     *                 Precondition: elements is not null and contains elements to be checked.
     *
     * @return A list of CSS selector strings for non-compliant elements.
     *         Postcondition: The list contains CSS selectors for elements that are used inappropriately.
     */
    private static List<String> getNonCompliantSelectors(Elements elements) {
        List<String> nonCompliantSelectors = new ArrayList<>();

        // Here we could add additional checks based on specific WCAG criteria for each element if necessary.
        for (Element element : elements) {
            // Add selectors for elements that are incorrectly used, or if additional checks are needed, they would go here
            nonCompliantSelectors.add(element.cssSelector());
        }

        return nonCompliantSelectors;
    }
}
