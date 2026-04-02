package com.looksee.pageBuilder.models.journeys;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.pageBuilder.models.LookseeObject;



/**
 * Represents the series of steps taken for an end to end journey
 */
public class DomainMap extends LookseeObject {

	@Relationship(type = "CONTAINS")
	private List<Journey> journeys;
		
	public DomainMap() {
		setJourneys(new ArrayList<>());
		setKey(generateKey());
	}
	
	public DomainMap(List<Journey> journeys) {
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
	
	public List<Journey> getJourneys() {
		return journeys;
	}

	public void setJourneys(List<Journey> journeys) {
		this.journeys = journeys;
	}

	public boolean addJourney(Journey journey) {
		return this.journeys.add(journey);
	}
}
