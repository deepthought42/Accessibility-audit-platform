package com.crawlerApi.generators;

/**
 * Interface for classes used for generating assorted values.
 * 
 * @author Brandon Kindred
 *
 * @param <T>	The object type for value to be generated
 */
public interface IFieldGenerator<T> {
	public T generateValue();
}
