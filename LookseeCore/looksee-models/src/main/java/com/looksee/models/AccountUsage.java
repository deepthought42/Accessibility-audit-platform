package com.looksee.models;

import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the usage of an account
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountUsage {
	private int discoveryLimit;
	private int discoveriesUsed;
	private int testLimit;
	private int testsUsed;
	private int totalDiscoveredTestCount;
	
	private int domainDiscoveries;
	private int domainTotalTestsRun;
	private int domainTotalDiscoveredTestCount;
	
	private Date currentDiscoveryStart;
	private long discoveryRunTime;
	private Date timestampOfLastDiscoveredTest;
	
	/**
	 * Constructs an {@link AccountUsage} object
	 * 
	 * @param discoveryLimit the discovery limit
	 * @param discoveriesUsed the discoveries used
	 * @param testLimit the test limit
	 * @param testsUsed the tests used
	 *
	 * precondition: discoveryLimit != null
	 * precondition: discoveriesUsed != null
	 * precondition: testLimit != null
	 * precondition: testsUsed != null
	 */
	public AccountUsage(int discoveryLimit,
						int discoveriesUsed,
						int testLimit,
						int testsUsed){
		this.setDiscoveryLimit(discoveryLimit);
		this.setDiscoveriesUsed(discoveriesUsed);
		this.setTestLimit(testLimit);
		this.setTestsUsed(testsUsed);
	}

	/**
	 * Constructs an {@link AccountUsage} object
	 *
	 * @param discoveryLimit the discovery limit
	 * @param discoveriesUsed the discoveries used
	 * @param testLimit the test limit
	 * @param testsUsed the tests used
	 * @param totalDiscoveredTestCount the total discovered test count
	 * @param domainDiscoveries the domain discoveries
	 * @param domainTotalDiscoveredTestCount the domain total discovered test count
	 * @param domainTotalTestsRun the domain total tests run
	 * @param currentDiscoveryStart the current discovery start
	 * @param discoveryRunTime the discovery run time
	 * @param timestampOfLastDiscoveredTest the timestamp of the last discovered test
	 *
	 * precondition: currentDiscoveryStart != null
	 * precondition: timestampOfLastDiscoveredTest != null
	 */
	public AccountUsage(int discoveryLimit,
						int discoveriesUsed,
						int testLimit,
						int testsUsed,
						int totalDiscoveredTestCount,
						int domainDiscoveries,
						int domainTotalDiscoveredTestCount,
						int domainTotalTestsRun,
						Date currentDiscoveryStart,
						long discoveryRunTime,
						Date timestampOfLastDiscoveredTest){
		assert currentDiscoveryStart != null : "currentDiscoveryStart must not be null";
		assert timestampOfLastDiscoveredTest != null : "timestampOfLastDiscoveredTest must not be null";

		this.setDiscoveryLimit(discoveryLimit);
		this.setDiscoveriesUsed(discoveriesUsed);
		this.setTestLimit(testLimit);
		this.setTestsUsed(testsUsed);
		this.setTotalDiscoveredTestCount(totalDiscoveredTestCount);
		this.setDomainDiscoveries(domainDiscoveries);
		this.setDomainTotalDiscoveredTestCount(domainTotalDiscoveredTestCount);
		this.setDomainTotalTestsRun(domainTotalTestsRun);
		this.setCurrentDiscoveryStart(currentDiscoveryStart);
		this.setDiscoveryRunTime(discoveryRunTime);
		this.setTimestampOfLastDiscoveredTest(timestampOfLastDiscoveredTest);
	}
}
