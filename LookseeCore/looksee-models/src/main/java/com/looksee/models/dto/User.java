package com.looksee.models.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * User DTO
 */
@Getter
@Setter
public class User{
	private String email;
	private String name;
	private String id;
	private String companyName;
	private String companyDomain;
	
	/**
	 * Constructor for {@link User}
	 * @param email the email of the user
	 * @param name the name of the user
	 *
	 * precondition: email != null
	 * precondition: name != null
	 */
	public User(String email,
				String name) {
		assert email != null;
		assert name != null;

		setEmail(email);
		setName(name);
	}
}
