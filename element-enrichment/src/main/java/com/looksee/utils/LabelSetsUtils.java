package com.looksee.utils;

import org.springframework.stereotype.Service;

import com.looksee.pageBuilder.models.enums.FormType;


@Service
public class LabelSetsUtils {

	public static FormType[] getFormTypeOptions() {
		return FormType.values();
	}
}
