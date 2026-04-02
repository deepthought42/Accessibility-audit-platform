package com.looksee.pageBuilder.models;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * Defines user information that can be used during testing and discovery
 */
@Node
public class TestUser extends LookseeObject{

	private String username;
	private String password;
	
	public TestUser(){}
	
	public TestUser(String username, String password){
		setUsername(username);
		setPassword(password);
		setKey(generateKey());
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String getPassword(){
		return this.password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String generateKey() {
		return "user"+username+password;
	}
}
