package com.looksee.browsing;

import java.util.ArrayList;
import java.util.Random;


/**
 * Encapsulates the possible values for a value field as a domain of values.
 */
public class ValueDomain {
	
	private ArrayList<String> values = new ArrayList<String>();
	private String alphabet="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String specialCharacters = alphabet+"+={}/\\:;!@#$%^&*()~|<>?[]-_";
	
	public ValueDomain(){
		//add empty string as bare minimum
		values.add("");
	}
	
	/**
	 * Generate random values of real numbers, decimals, alphabetic sequences 
	 *   and alphabetic sequences with special characters
	 */
	public void generateAllValueTypes(){
		addRandomRealNumbers();
		addRandomDecimals();
		addRandomAlphabeticStrings();
		addRandomSpecialCharacterAlphabeticStrings();
	}
	
	/**
	 * Adds 100 random long values to values of domain
	 */
	public void addRandomRealNumbers(){
		Random random = new Random();
		for(int i= 0; i< 100; i++){
			values.add(Long.toString(random.nextLong()));
		}
	}
	
	/**
	 * Adds 100 random decimal values to values of domain
	 */
	public void addRandomDecimals(){
		Random random = new Random();
		
		for(int i= 0; i< 100; i++){
			values.add(Long.toString(random.nextLong()) + Long.toString(random.nextLong()));
		}
	}
	
	/**
	 * Adds 100 random alphabetic strings to values of domain
	 */
	public void addRandomAlphabeticStrings(){
		Random random = new Random();
		
		for(int i= 0; i< 100; i++){
			StringBuilder valueSeq = new StringBuilder();
			
			for(long j = 0L; j< random.nextLong(); j++){
				int  n = random.nextInt(51) + 1;
				valueSeq.append(alphabet.charAt(n));
			}
			values.add(valueSeq.toString());
		}
	}
	

	/**
	 * Adds 100 random specialCharacter strings to values of domain
	 */
	public void addRandomSpecialCharacterAlphabeticStrings(){
		Random random = new Random();
		
		for(int i= 0; i< 10; i++){
			StringBuilder valueSeq = new StringBuilder();
			
			for(int j = 0; j< random.nextInt(); j++){
				int  n = random.nextInt(78) + 1;
				valueSeq.append(specialCharacters.charAt(n));
			}
			values.add(valueSeq.toString());
		}
	}
	
	public ArrayList<String> getValues(){
		return this.values;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){

		return "";
	}
}
