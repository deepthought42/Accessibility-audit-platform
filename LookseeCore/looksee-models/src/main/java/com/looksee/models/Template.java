package com.looksee.models;

import com.looksee.models.enums.TemplateType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;


/**
 * A Template is defined as a semi-generic string that matches a set of {@link Element}s.
 *
 * invariant: type != null
 * invariant: template != null
 * invariant: elements != null
 */
@Getter
@Setter
@Node
public class Template extends LookseeObject {

	private String type;
	private String template;
	
	@Relationship(type = "MATCHES")
	private List<Element> elements;
	
	/**
	 * Constructs a new {@link Template}
	 */
	public Template(){
		setType(TemplateType.UNKNOWN);
		setTemplate("");
		setElements(new ArrayList<>());
		setKey(generateKey());
	}
	
	/**
	 * Constructs a new {@link Template}
	 *
	 * @param type the type of the template
	 * @param template the template of the template
	 *
	 * precondition: type != null
	 * precondition: template != null
	 */
	public Template(TemplateType type, String template){
		assert type != null;
		assert template != null;
		setType(type);
		setTemplate(template);
		setElements(new ArrayList<>());
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return type+org.apache.commons.codec.digest.DigestUtils.sha256Hex(template);
	}

	/**
	 * Gets the type of the template
	 *
	 * @return the type of the template
	 */
	public TemplateType getType() {
		return TemplateType.create(type);
	}

	/**
	 * Sets the type of the template
	 *
	 * @param type the type of the template
	 *
	 * precondition: type != null
	 */
	public void setType(TemplateType type) {
		assert type != null;
		this.type = type.toString();
	}
}
