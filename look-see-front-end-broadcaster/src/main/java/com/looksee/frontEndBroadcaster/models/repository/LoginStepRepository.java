package com.looksee.frontEndBroadcaster.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.journeys.LoginStep;


@Repository
public interface LoginStepRepository extends Neo4jRepository<LoginStep, Long> {

	@Query("MATCH (step:LoginStep{key:$step_key}) RETURN step")
	public LoginStep findByKey(@Param("step_key") String step_key);

}
