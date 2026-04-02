package com.looksee.pageBuilder.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

/**
 * Encompasses a domain name as well as all {@link Test}s and {@link Group}s 
 * belong to this domain
 */
public class Domain extends LookseeObject{
	
	private String url;
	private String logoUrl;
	private String entrypointUrl;
	private List<String> sitemap;
	
	@Relationship(type = "HAS")
	private List<PageState> pages;
	
	@Relationship(type = "HAS")
	private Set<DomainAuditRecord> audit_records;

	@Relationship(type="USES")
	private DesignSystem design_system;
	
	/**
	 * 
	 * 
	 * @param domain
	 * @param organization
	 */
	public Domain(){
		setPages( new ArrayList<>() );
		setAuditRecords(new HashSet<>());
		setSitemap(new ArrayList<>());
		setDesignSystem(new DesignSystem());
	}
	
	/**
	 * 
	 * @param protocol web protocol ("http", "https", "file", etc.)
	 * @param path landable url
	 * @param browser name of the browser ie. chrome, firefox, etc.
	 * @param logo_url url of logo image file
	 */
	public Domain( String protocol, 
				   String host, 
				   String path, 
				   String logo_url
	){
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

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logo_url) {
		this.logoUrl = logo_url;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "domain"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(getUrl());
	}

	public boolean addPage(PageState page) {
		//check if page state exists
		for(PageState state : this.getPages()){
			if(state.getKey().equals(page.getKey())){
				return false;
			}
		}
		
		return this.getPages().add(page);
	}

	public List<PageState> getPages() {
		return this.pages;
	}
	
	public void setPages(List<PageState> pages) {
		this.pages = pages;
	}

	public Set<DomainAuditRecord> getAuditRecords() {
		return audit_records;
	}

	public void setAuditRecords(Set<DomainAuditRecord> audit_records) {
		this.audit_records = audit_records;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getSitemap() {
		return sitemap;
	}

	public void setSitemap(List<String> sitemap) {
		this.sitemap = sitemap;
	}

	public String getEntrypointUrl() {
		return entrypointUrl;
	}

	public void setEntrypointUrl(String entrypoint_url) {
		this.entrypointUrl = entrypoint_url;
	}

	public DesignSystem getDesignSystem() {
		return design_system;
	}

	public void setDesignSystem(DesignSystem design_system) {
		this.design_system = design_system;
	}
}
