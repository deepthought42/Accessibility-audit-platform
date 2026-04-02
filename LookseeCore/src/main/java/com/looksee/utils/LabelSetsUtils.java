package com.looksee.utils;

import org.springframework.stereotype.Service;

import com.looksee.models.enums.FormType;

import lombok.NoArgsConstructor;

/**
 * Utility class for label sets.
 */
@NoArgsConstructor
@Service
public class LabelSetsUtils {

	/**
	 * Returns the form type options.
	 * @return the form type options
	 */
	public static FormType[] getFormTypeOptions() {
		return FormType.values();
	}
}
