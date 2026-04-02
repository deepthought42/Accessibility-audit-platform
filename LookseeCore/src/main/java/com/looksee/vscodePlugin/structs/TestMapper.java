package com.looksee.vscodePlugin.structs;

import com.looksee.models.Test;
import java.util.HashMap;
import java.util.Map;


/**
 * Holds elements in a map, that is keyed by element action sequence and content. 
 *
 */
public class TestMapper {
	private Map<String, Test> testHash;
	
	/**
	 * Creates a new instance of the tracker
	 */
	public TestMapper(){
		testHash = new HashMap<String, Test>();
	}
	
	/**
	 * Adds a new entry to the element action path
	 * 
	 * @param test {@link Test} to add
	 * 
	 * precondition: test != null
	 */
	public void addTest(Test test){
		assert test != null : "test must not be null";
		int hash_code = test.hashCode();
		
		if(!this.testHash.containsKey(Integer.toString(hash_code))){
			this.testHash.put(Integer.toString(hash_code), test);
		}
	}
	
	/**
	 * Checks if element action sequence exists.
	 * 
	 * @param test {@link Test} to check if exists
	 * @return true if the test exists, false otherwise
	 * 
	 * precondition: test != null
	 */
	public boolean containsTest(Test test){
		assert test != null : "test must not be null";
		String hash_key = Integer.toString(test.hashCode());
		return  this.testHash.containsKey(hash_key);
	}
	
	/**
	 * Gets the hash of element action sequences
	 * 
	 * @return hash of element action sequences
	 */
	public Map<String, Test> getTestHash(){
		return testHash;
	}
}
