package com.looksee.pageBuilder.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.pageBuilder.models.enums.TemplateType;


/**
 * 		A Template is defined as a semi-generic string that matches a set of {@link Element}s
 */
@Node
public class Template extends LookseeObject {

	private String type;
	private String template;
	
	@Relationship(type = "MATCHES")
	private List<Element> elements;
	
	public Template(){
		setType(TemplateType.UNKNOWN);
		setTemplate("");
		setElements(new ArrayList<>());
		setKey(generateKey());
	}
	
	public Template(TemplateType type, String template){
		setType(type);
		setTemplate(template);
		setElements(new ArrayList<>());
		setKey(generateKey());
	}
	
	@Override
	public String generateKey() {
		return type+org.apache.commons.codec.digest.DigestUtils.sha256Hex(template);
	}

	public TemplateType getType() {
		return TemplateType.create(type);
	}

	public void setType(TemplateType type) {
		this.type = type.toString();
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public List<Element> getElements() {
		return elements;
	}

	public void setElements(List<Element> elements) {
		this.elements = elements;
	}
}
