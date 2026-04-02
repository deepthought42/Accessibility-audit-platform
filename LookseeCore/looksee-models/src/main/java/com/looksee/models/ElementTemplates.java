package com.looksee.models;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ElementTemplates is a class that represents a list of elements and templates
 */
@Getter
@Setter
@NoArgsConstructor
public class ElementTemplates {
	private List<Element> elements;
	private List<Template> templates;
}
