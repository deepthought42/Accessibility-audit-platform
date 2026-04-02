package com.looksee.models.repository;

import com.looksee.models.journeys.LandingStep;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link LandingStep} objects
 */
@Repository
public interface LandingStepRepository extends Neo4jRepository<LandingStep, Long> {

	/**
	 * Finds a landing step by its key
	 *
	 * @param step_key the key of the landing step
	 * @return the landing step
	 */
	@Query("MATCH (step:LandingStep{key:$step_key}) RETURN step LIMIT 1")
	public LandingStep findByKey(@Param("step_key") String step_key);
}
