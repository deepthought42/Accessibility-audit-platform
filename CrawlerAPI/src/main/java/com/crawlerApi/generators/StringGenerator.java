package com.crawlerApi.generators;

import java.util.Random;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class StringGenerator implements IFieldGenerator<String> {

	private String alphabet="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String specialCharacters = alphabet+"+={}/\\:;!@#$%^&*()~|<>?[]-_";
	
	
	public StringGenerator() {

	}

	public String generateValue() {
		Random random = new Random();
		
		StringBuilder valueSeq = new StringBuilder();
		
		for(int j = 0; j< random.nextInt(); j++){
			int  n = random.nextInt(78) + 1;
			valueSeq.append(specialCharacters.charAt(n));
		}
		
		return valueSeq.toString();
	}

}
