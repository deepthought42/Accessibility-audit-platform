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
 * Audits audio and video elements for WCAG 2.1 Section 1.4.2 compliance, checking
 * that autoplaying media has user controls.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
@Component
public class AudioControlAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

	List<String> bad_link_text_list;
	
	public AudioControlAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Audits audio and video elements on the page for autoplay control compliance
	 * per WCAG 2.1 Section 1.4.2. Identifies autoplaying media that lacks user
	 * controls or mute options.
	 *
	 * @pre {@code page_state != null} - the page state to audit must be provided
	 * @pre {@code audit_record != null} - the parent audit record must be provided
	 * @post returned {@code Audit} is non-null and has been persisted
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
								 AuditName.AUDIO_CONTROL,
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
     * Evaluates an HTML document for compliance with WCAG 2.1 Section 1.4.2 - Audio Control.
     * This section requires that if any audio on a web page plays automatically for more than 3 seconds,
     * the user should be able to pause, stop, or control the volume of the audio independently from the system volume.
     *
     * @pre {@code document != null} - the HTML document to evaluate must be provided
     * @post returned list is non-null (may be empty if no issues are found)
     *
     * @param document The parsed Jsoup Document to check.
     * @return A list of GenericIssue objects representing any issues found.
     * @throws IllegalArgumentException if the provided document is null.
     */
    public static List<GenericIssue> checkCompliance(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document must not be null");
        }
        List<GenericIssue> issues = new ArrayList<>();

        // Select all audio and video elements
        Elements audioElements = document.select("audio[autoplay], video[autoplay]");

        for (Element element : audioElements) {
            boolean hasControls = element.hasAttr("controls");
            boolean hasNoAudio = element.hasAttr("muted");

            if (!hasControls && !hasNoAudio) {
                issues.add(new GenericIssue(
                        "Autoplaying audio or video found without user controls or mute options.",
                        "Audio Control Violation",
                        element.cssSelector(),
                        "Ensure that audio or video elements with autoplay have user controls or are muted."
                ));
            }
        }

        // Check for embedded content (iframes) that might include autoplaying audio
        Elements iframes = document.select("iframe");

        for (Element iframe : iframes) {
            if (iframe.hasAttr("src")) {
                String src = iframe.attr("src");
                if (src.contains("autoplay=1")) {
                    issues.add(new GenericIssue(
                            "Embedded content with autoplaying audio or video found.",
                            "Audio Control Violation",
                            iframe.cssSelector(),
                            "Ensure embedded content with autoplay has user controls or the autoplay feature is disabled."
                    ));
                }
            }
        }

        return issues;
    }
}
