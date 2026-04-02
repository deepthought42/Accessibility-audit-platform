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
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;

/**
 * Audits HTML table elements for WCAG 2.1 Section 1.3.1 compliance, checking header
 * presence, scope attributes, and header-data cell associations.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class TableStructureAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public TableStructureAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 *
	 * Scores table semantics on a page to assess WCAG table structure conformance.
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
        List<Element> tables = jsoup_doc.getElementsByTag("table");
        List<GenericIssue> issues = new ArrayList<>();
        for(Element table: tables){
            issues.addAll(validateTable(page_state.getId(), table, labels));
        }
        
        for(GenericIssue issue: issues){
            ElementState element_state = elementStateService.findByPageAndCssSelector(page_state.getId(), issue.getCssSelector());

            int score = issue.getRecommendation().isEmpty() ? 1 : 0;

            UXIssueMessage issue_msg = new ElementStateIssueMessage(Priority.HIGH,
                                                                issue.getDescription(),
                                                                issue.getRecommendation(),
                                                                element_state,
                                                                AuditCategory.ACCESSIBILITY,
                                                                labels,
                                                                ada_compliance,
                                                                issue.getTitle(),
                                                                score,
                                                                1);
            issue_messages.add(issue_msg);
        }

		String why_it_matters = "Structured table semantics are essential for users of assistive technologies. Proper use of table headers and associations enables screen readers to communicate relationships between cells and meaningfully navigate tabular data.";
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		String description = "Data tables should expose clear header relationships and semantic structure.";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								 AuditSubcategory.NAVIGATION,
								 AuditName.TABLE_STRUCTURE,
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
     * Validates a single HTML table element for WCAG 2.1 Section 1.3.1 compliance.
     *
     * @pre {@code table != null}
     * @pre {@code labels != null}
     * @post returned list is non-null
     * @param page_id the page identifier
     * @param table The table element to validate.
     * @param labels the label set for categorization
     * @return list of issues found
     */
    public static List<GenericIssue> validateTable(long page_id, Element table, Set<String> labels) {
        Objects.requireNonNull(table, "table must not be null");
        Objects.requireNonNull(labels, "labels must not be null");

        List<GenericIssue> issues = new ArrayList<>();
        // Select all <th> elements (table headers) in the table
        Elements thElements = table.select("th");

        // Check if the table has any <th> elements
        if (thElements.isEmpty()) {
            String description = "Table headers are crucial for WCAG 2.1 Section 1.3.1 because they provide essential structure and context to data tables, ensuring that the relationships between data cells and their corresponding headers are clear. This is particularly important for users who rely on assistive technologies, like screen readers, to navigate and understand the content. Headers help users interpret the data by conveying the meaning and relationship of data cells, allowing screen readers to announce the headers in context as users move through the table. Without properly marked headers, tables can become confusing and inaccessible, making it difficult for users with disabilities to understand the information presented. Proper use of <th>, scope, and headers attributes ensures that all users, regardless of their abilities, can access and comprehend the data within a table.";
            String recommendation = "Ensure that all data tables use <th> elements to define headers for each column or row. These <th> elements should include the scope attribute to clearly indicate whether they serve as column, row, or group headers.";
            String title = "Table without <th> elements defined";
            String xpath = BrowserService.getXPath(table);
            String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
            issues.add(new GenericIssue(description, title, cssSelector, recommendation));
        } else {
            String description = "Table headers are crucial for WCAG 2.1 Section 1.3.1 because they provide essential structure and context to data tables, ensuring that the relationships between data cells and their corresponding headers are clear. This is particularly important for users who rely on assistive technologies, like screen readers, to navigate and understand the content. Headers help users interpret the data by conveying the meaning and relationship of data cells, allowing screen readers to announce the headers in context as users move through the table. Without properly marked headers, tables can become confusing and inaccessible, making it difficult for users with disabilities to understand the information presented. Proper use of <th>, scope, and headers attributes ensures that all users, regardless of their abilities, can access and comprehend the data within a table.";

            // Check each <th> element for a 'scope' attribute
            for (Element th : thElements) {
                String scope = th.attr("scope");
                if (scope.isEmpty()) {
                    String recommendation = "Ensure that all data tables use <th> elements to define headers for each column or row. These <th> elements should include the scope attribute to clearly indicate whether they serve as column, row, or group headers.";
                    String title = "<th> element without a scope attribute";
                    String xpath = BrowserService.getXPath(th);
                    String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                    issues.add(new GenericIssue(description, title, cssSelector, recommendation));
                } else {
                    String recommendation = "";
                    String title = "<th> has scope attribute defined!";
                    String xpath = BrowserService.getXPath(th);
                    String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                    issues.add(new GenericIssue(description, title, cssSelector, recommendation));
                }
            }
        }
        // Check for headers/id attributes in <td> elements for complex tables
        Elements tdElements = table.select("td");
        for (Element td : tdElements) {
            String headers = td.attr("headers");
            if (!headers.isEmpty()) {
                // Ensure that each 'headers' value corresponds to a <th> element with a matching id
                for (String headerId : headers.split(" ")) {
                    List<Element> table_headers = table.select("#" + headerId);
                    if (table_headers.isEmpty()) {
                        String description = "It's important for the headers attribute on <td> tags to reference a valid <th> element with a matching id because it ensures that assistive technologies can accurately interpret the relationship between data cells and their corresponding headers. This is crucial for users who rely on screen readers to navigate tables, as it allows them to understand the context and structure of the data. According to WCAG 2.1 Section 1.3.1, this practice ensures that all users, regardless of their abilities, can access and comprehend the information presented in the table. Without valid references, the table's meaning could become unclear, leading to confusion and reduced accessibility.";
                        String recommendation = "To fix <td> headers attributes that don't point to valid header IDs, first ensure that each <th> element in your table has a unique id attribute. Then, update the headers attribute on the corresponding <td> elements to match these id values. This will correctly link each data cell to its associated header, ensuring compliance with WCAG 2.1 Section 1.3.1 and improving the accessibility of your table for users with assistive technologies.";
                        String title = "No corresponding <th> with id '" + headerId + "' found.";
                        
                        String xpath = BrowserService.getXPath(td);
                        String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                        issues.add(new GenericIssue(description, title, cssSelector, recommendation));
                    }
                    else{
                        String description = "It's important for the headers attribute on <td> tags to reference a valid <th> element with a matching id because it ensures that assistive technologies can accurately interpret the relationship between data cells and their corresponding headers. This is crucial for users who rely on screen readers to navigate tables, as it allows them to understand the context and structure of the data. According to WCAG 2.1 Section 1.3.1, this practice ensures that all users, regardless of their abilities, can access and comprehend the information presented in the table. Without valid references, the table's meaning could become unclear, leading to confusion and reduced accessibility.";
                        String recommendation = "";
                        String title = "Table data cell is associated with a valid header";
                        
                        String xpath = BrowserService.getXPath(td);
                        String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                        issues.add(new GenericIssue(description, title, cssSelector, recommendation));
                    }
                }
            }
            else{
                //headers is empty
                String description = "The headers attribute on <td> elements is crucial for WCAG 2.1 Section 1.3.1 compliance because it links data cells to their corresponding headers in complex tables. This ensures that assistive technologies can accurately convey the relationships between data and headers, making the table content accessible and understandable for users with disabilities. Without this attribute, the table's structure and meaning may be unclear, leading to accessibility issues.";
                String recommendation = "To fix <td> elements that lack headers attributes, identify the corresponding <th> elements that act as headers for each data cell. Assign unique id attributes to these <th> elements and then add the headers attribute to each <td>, referencing the relevant id values. This will establish a clear relationship between the data cells and their headers, ensuring compliance with WCAG 2.1 Section 1.3.1 and improving accessibility.";
                String title = "No headers attribute was found for <td> element";
                String xpath = BrowserService.getXPath(table);
                String cssSelector = BrowserService.generateCssSelectorFromXpath(xpath);
                issues.add(new GenericIssue(description, title, cssSelector, recommendation));
            }
        }

        return issues;
    }
}
