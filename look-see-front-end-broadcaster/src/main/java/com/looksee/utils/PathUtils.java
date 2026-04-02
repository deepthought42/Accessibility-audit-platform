package com.looksee.utils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.frontEndBroadcaster.models.LookseeObject;
import com.looksee.frontEndBroadcaster.models.PageState;
import com.looksee.frontEndBroadcaster.models.journeys.Step;


public class PathUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathUtils.class);
	
	/**
	 * Retrieves the last {@link PageState} in the given list of {@link LookseeObject}s
	 * 
	 * @param pathObjects list of {@link LookseeObject}s in sequential order
	 * 
	 * @return last page state in list
	 * 
	 * @pre pathObjects != null
	 */
	public static PageState getLastPageState(List<Step> steps) {
		assert(steps != null);
			
		//get last step
		Step last_step = steps.get(steps.size()-1);
		PageState last_page = last_step.getEndPage();
		
		return last_page;
	}
	
	/**
	 * Retrieves the last {@link PageState} in the given list of {@link LookseeObject}s
	 * 
	 * @param pathObjects list of {@link LookseeObject}s in sequential order
	 * 
	 * @return last page state in list
	 * 
	 * @pre pathObjects != null
	 */
	public static PageState getSecondToLastPageState(List<Step> steps) {
		assert(steps != null);
			
		//get last step
		Step last_step = steps.get(steps.size()-1);
		PageState start_page = last_step.getStartPage();
		
		return start_page;
	}
}
