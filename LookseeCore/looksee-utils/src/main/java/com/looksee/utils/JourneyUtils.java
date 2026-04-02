package com.looksee.utils;

import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.Step;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility class for journey operations
 */
public class JourneyUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyUtils.class.getName());

	/**
	 * Checks if a list of {@link Step steps} has a {@linkplain LoginStep}
	 * @param steps the list of steps
	 * @return true if the list of steps has a {@linkplain LoginStep}, otherwise false
	 *
	 * precondition: steps != null
	 */
	public static boolean hasLoginStep(List<Step> steps) {
		assert steps != null;

		for(Step step: steps) {
			if(step instanceof LoginStep) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Trims the pre-login steps from a list of steps
	 * @param steps the list of steps
	 * @return the list of steps without the pre-login steps
	 *
	 * precondition: steps != null
	 * precondition: !steps.isEmpty()
	 */
	public static List<Step> trimPreLoginSteps(List<Step> steps) {
		assert steps != null;
		assert !steps.isEmpty();
		
		int login_step_index = -1;
		
		for(int idx=0; idx < steps.size(); idx++) {
			if(steps.get(idx) instanceof LoginStep) {
				login_step_index = idx;
				break;
			}
		}
				
		if(login_step_index < 0) {
			return new ArrayList<>();
		}
		
		return steps.subList(login_step_index, steps.size());
	}

	/**
	 * Checks the url of the start page for a {@link Step} agains a list of {@link Step}
	 * to ensure that the page url hasn't already been expanded
	 *
	 * @param steps the list of steps
	 * @param step the step to check for
	 * @return true if page url exists in list, otherwise false
	 *
	 * precondition: steps != null
	 * precondition: step != null
	 */
	public static boolean hasStartPageAlreadyBeenExpanded(List<Step> steps, Step step) {
		assert steps != null;
		assert step != null;

		for(Step list_step: steps) {
			if(list_step.getStartPage().getUrl().equals(step.getStartPage().getUrl())) {
				return true;
			}
		}
		
		return false;
	}
}
