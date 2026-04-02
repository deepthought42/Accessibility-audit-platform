package com.looksee.models.dto;

import com.looksee.models.Test;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer object for Test information to be relayed to user ide
 */
@Getter
@Setter
public class TestCreatedDto {

	private String key;
	private String name;

	/**
	 * Constructor for {@link TestCreatedDto}
	 * @param test the {@link Test} to convert
	 *
	 * precondition: test != null
	 */
	public TestCreatedDto(Test test){
		assert test != null;

		setKey(test.getKey());
		setName(test.getName());
	}
}
