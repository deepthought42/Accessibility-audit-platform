package com.looksee.models.repository;

import com.looksee.models.Animation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for interacting with {@link Animation} objects
 */
@Repository
public interface AnimationRepository extends Neo4jRepository<Animation, Long> {
	/**
	 * Finds an {@link Animation} by key
	 * 
	 * @param key key of the animation to find
	 * @return {@link Animation}
	 * 
	 * precondition: key != null
	 */
	public Animation findByKey(@Param("key") String key);
}
