package com.crawlerApi.generators;

import java.util.Random;

/**
 * Generates Decimal values 
 * @author Brandon Kindred
 *
 */
public class DecimalGenerator implements IFieldGenerator<Double> {

	public DecimalGenerator() {
	}

	/**
	 * Generate double value by appending 2 integers
	 */
	public Double generateValue() {
		Random random = new Random();
		return Double.parseDouble(random.nextInt() + "." + random.nextInt());
	}
}
