package com.looksee.models;

import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Represents a domain.
 *
 * <p>This class represents a domain and contains a list of {@link PageState} objects,
 * which represent the pages of the domain. It also contains a {@link DesignSystem} object,
 * which represents the design system of the domain.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: url is not null after construction with parameters</li>
 *   <li>invariant: pages list is never null</li>
 *   <li>invariant: auditRecords set is never null</li>
 *   <li>invariant: sitemap list is never null</li>
 *   <li>invariant: designSystem is never null</li>
 * </ul>
 */
@Getter
@Setter
public class Domain extends LookseeObject{

	private String url;
	private String logoUrl;
	private String entrypointUrl;
	private List<String> sitemap;

	@Relationship(type = "HAS")
	private List<PageState> pages;
	
	@Relationship(type = "HAS")
	private Set<DomainAuditRecord> auditRecords;

	@Relationship(type="USES")
	private DesignSystem designSystem;
	
	/**
	 * Constructs a new {@link Domain}
	 */
	public Domain(){
		setPages( new ArrayList<>() );
		setAuditRecords(new HashSet<>());
		setSitemap(new ArrayList<>());
		setDesignSystem(new DesignSystem());
	}
	
	/**
	 * Constructs a new {@link Domain} with the given parameters.
	 *
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param host domain name
	 * @param path landable url path
	 * @param logo_url url of logo image file
	 *
	 * precondition: protocol != null;
	 * precondition: host != null;
	 * precondition: path != null;
	 * precondition: logo_url != null;
	 */
	public Domain( String protocol,
					String host,
					String path,
					String logo_url
	){
		assert protocol != null;
		assert host != null;
		assert path != null;
		assert logo_url != null;

		setLogoUrl(logo_url);
		setUrl(host);
		setEntrypointUrl(host+path);
		setPages(new ArrayList<>());
		setAuditRecords(new HashSet<>());
		setDesignSystem(new DesignSystem());
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Domain){
			Domain domain = (Domain)o;
			if(domain.getUrl().equals(this.getUrl())){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "domain"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getUrl());
	}

	/**
	 * Adds a page to the domain.
	 *
	 * @param page The page to add
	 * @return true if the page was added successfully, false otherwise
	 *
	 * precondition: page != null
	 */
	public boolean addPage(PageState page) {
		assert page != null;
		//check if page state exists
		for(PageState state : this.getPages()){
			if(state.getKey().equals(page.getKey())){
				return false;
			}
		}
		
		return this.getPages().add(page);
	}
}
