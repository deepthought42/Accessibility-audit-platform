package com.looksee.browsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.CSSProperty.LineHeight;
import cz.vutbr.web.css.CSSProperty.Margin;

public class CssPropertyFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(CssPropertyFactory.class);
	
	public static String construct(CSSProperty property) {
		log.warn(property.getClass().getName());
		if(property instanceof Margin) {
			Margin margin = (Margin)property;
			return margin.toString();
		}
		else if(property instanceof LineHeight) {
			LineHeight line_height = (LineHeight)property;
			return line_height.toString();
		}
		// TODO Auto-generated method stub
		return property.toString();
	}


}
