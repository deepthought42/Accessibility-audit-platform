package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Defines user information that can be used during testing and discovery.
 *
 * invariant: username != null
 * invariant: password != null
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class TestUser extends LookseeObject{

	private String username;
	private String password;
	
	/**
	 * Constructs a new {@link TestUser}
	 *
	 * @param username the username of the test user
	 * @param password the password of the test user
	 *
	 * precondition: username != null
	 * precondition: password != null
	 */
	public TestUser(String username, String password){
		assert username != null;
		assert password != null;
		setUsername(username);
		setPassword(password);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "user"+username+password;
	}
}
