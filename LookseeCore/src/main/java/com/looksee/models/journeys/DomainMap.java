package com.looksee.models.journeys;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.models.LookseeObject;

import lombok.Getter;
import lombok.Setter;

	/**
 * Represents a domain map, which is a collection of journeys that are related
 * to a specific domain.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>journeys is never null (initialized to empty list by default).</li>
 * </ul>
 */
public class DomainMap extends LookseeObject {

	/**
	 * The journeys that are related to the domain map
	 */
	@Getter
	@Setter
	@Relationship(type = "CONTAINS")
	private List<Journey> journeys;

	/**
	 * Creates a new domain map
	 */
	public DomainMap() {
		setJourneys(new ArrayList<>());
		setKey(generateKey());
	}

	/**
	 * Creates a new domain map with the given journeys
	 * @param journeys the journeys to add to the domain map
	 * @throws IllegalArgumentException if the journeys are null
	 *
	 * precondition: journeys != null
	 */
	public DomainMap(List<Journey> journeys) {
		assert journeys != null;
		setJourneys(journeys);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "journey"+UUID.randomUUID();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DomainMap clone() {
		return new DomainMap(new ArrayList<>(getJourneys()));
	}
	
	/**
	 * Adds a journey to the domain map
	 * @param journey the journey to add
	 * @return true if the journey was added, false otherwise
	 *
	 * precondition: journey != null
	 */
	public boolean addJourney(Journey journey) {
		assert journey != null;
		return this.journeys.add(journey);
	}
}
