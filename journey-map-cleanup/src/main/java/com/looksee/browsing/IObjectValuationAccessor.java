package com.looksee.browsing;

/**
 * Provides access to the cost and reward of an object
 * 
 * @author Brandon Kindred
 *
 */
public interface IObjectValuationAccessor {

	/**
	 * Computes and returns the cost of the current object
	 * 
	 * @return
	 */
	public abstract double getCost();
	
	/**
	 * Computes and returns the reward for the current object
	 * 
	 * @return
	 */
	public abstract double getReward();
}
