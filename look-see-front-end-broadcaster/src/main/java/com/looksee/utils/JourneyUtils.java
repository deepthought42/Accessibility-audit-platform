package com.looksee.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.frontEndBroadcaster.models.journeys.LoginStep;
import com.looksee.frontEndBroadcaster.models.journeys.Step;


public class JourneyUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyUtils.class.getName());

	/**
	 * Checks if a list of {@link Step steps} has a {@linkplain LoginStep}
	 * @param steps
	 * @return
	 */
	public static boolean hasLoginStep(List<Step> steps) {
		for(Step step: steps) {
			if(step instanceof LoginStep) {
				return true;
			}
		}
		
		return false;
	}

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
	 * @param new_steps
	 * @param step
	 * @return true if page url exists in list, otherwise false
	 */
	public static boolean hasStartPageAlreadyBeenExpanded(List<Step> steps, Step step) {
		for(Step list_step: steps) {
			if(list_step.getStartPage().getUrl().equals(step.getStartPage().getUrl())) {
				return true;
			}
		}
		
		return false;
	}

}
