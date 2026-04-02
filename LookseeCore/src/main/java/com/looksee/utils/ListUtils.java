package com.looksee.utils;

import java.util.ArrayList;
import java.util.List;

import com.looksee.models.journeys.Step;

import lombok.NoArgsConstructor;

/**
 * Utility class for list operations.
 */
@NoArgsConstructor
public class ListUtils {
	/**
	 * Clones a list of steps.
	 * @param steps_list the list of steps to clone
	 * @return the cloned list of steps
	 *
	 * precondition: steps_list != null
	 */
	public static List<Step> clone(List<Step> steps_list) {
		assert steps_list != null;

		List<Step> steps = new ArrayList<>();
		for(Step step : steps_list) {
			steps.add(step.clone());
		}
		
		return steps;
	}
}
