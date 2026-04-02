package com.looksee.audit.informationArchitecture.audits;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

/**
 * Audits page security for HTTPS/SSL compliance, checking whether the page connection is encrypted.
 *
 * <p><b>Class invariant:</b> {@code audit_service} and {@code issue_message_service} are non-null
 * after Spring dependency injection is complete.</p>
 */
@Component
public class SecurityAudit implements IExecutablePageStateAudit {
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	public SecurityAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Checks whether the page uses HTTPS encryption.
	 *
	 * @pre {@code page_state != null}
	 * @post returned {@code Audit} is non-null and persisted
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		String why_it_matters = "Sites that don't use HTTPS are highly insecure and are more likley to leak personal identifiable information(PII). Modern users are keenly aware of this fact and are less likely to trust sites that aren't secured.";
		Set<String> labels = new HashSet<>();
		labels.add("information_architecture");
		labels.add("security");
		
		boolean is_secure = page_state.isSecured();
		if(!is_secure) {
			String title = "Page isn't secure";
			String description = page_state.getUrl() + " doesn't use https";
			String wcag_compliance = "";
			String recommendation = "Enable encryption(SSL) for your site by getting a signed certificate from a certificate authority and enabling ssl on the server that hosts your website.";
			//Set<Recommendation> recommendations = new HashSet<>();
			//recommendations.add(new Recommendation(recommendation));
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.HIGH,
														description,
														ObservationType.SECURITY,
														AuditCategory.INFORMATION_ARCHITECTURE,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														0, 
														1, 
														recommendation);
			
			issue_messages.add(issue_message_service.save(ux_issue));
		}
		else {
			String title = "Page is secure";
			String description = page_state.getUrl() + " uses https protocol to provide a secure connection";
			String wcag_compliance = "";
			String recommendation = "";
			//Set<Recommendation> recommendations = new HashSet<>();
			//recommendations.add(new Recommendation(recommendation));
			
			UXIssueMessage ux_issue = new UXIssueMessage(Priority.NONE,
														description,
														ObservationType.SECURITY,
														AuditCategory.INFORMATION_ARCHITECTURE,
														wcag_compliance,
														labels,
														why_it_matters,
														title,
														1, 
														1, 
														recommendation);

			issue_messages.add(issue_message_service.save(ux_issue));
		}
		
		String description = "";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								AuditSubcategory.SECURITY,
								AuditName.ENCRYPTED,
								points_earned,
								issue_messages,
								AuditLevel.PAGE,
								max_points,
								page_state.getUrl(),
								why_it_matters,
								description,
								false);

		Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
		return audit_service.save(audit);
	}
	

	/**
	 * Returns a sorted list of distinct strings from the input.
	 *
	 * @pre {@code from != null}
	 * @post returned list contains only distinct, sorted elements
	 * @param from the source list
	 * @return a new sorted list of distinct strings
	 */
	public static List<String> makeDistinct(List<String> from){
		Objects.requireNonNull(from, "from must not be null");
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
}