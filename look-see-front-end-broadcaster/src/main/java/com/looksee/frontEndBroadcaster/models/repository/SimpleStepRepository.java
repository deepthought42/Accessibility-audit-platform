package com.looksee.frontEndBroadcaster.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.journeys.SimpleStep;

@Repository
public interface SimpleStepRepository extends Neo4jRepository<SimpleStep, Long> {

	@Query("MATCH (step:SimpleStep{key:$step_key}) RETURN step")
	public SimpleStep findByKey(@Param("step_key") String step_key);

}
