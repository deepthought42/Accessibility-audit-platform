package com.looksee.models.repository;

import com.looksee.models.ActionOLD;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for interacting with {@link ActionOLD} objects
 */
@Repository
public interface ActionRepository extends Neo4jRepository<ActionOLD, Long> {
	/**
	 * Finds an {@link ActionOLD} by key
	 * 
	 * @param key key of the action to find
	 * @return {@link ActionOLD}
	 * 
	 * precondition: key != null
	 */
	public	ActionOLD findByKey(@Param("key") String key);

}
