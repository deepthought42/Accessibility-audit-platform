package com.looksee.utils;

import java.util.ArrayList;
import java.util.List;

import com.looksee.pageBuilder.models.journeys.Step;


public class ListUtils {

	public static List<Step> clone(List<Step> steps_list) {
		List<Step> steps = new ArrayList<>();
		for(Step step : steps_list) {
			steps.add(step.clone());
		}
		
		return steps;
	}
	
}
