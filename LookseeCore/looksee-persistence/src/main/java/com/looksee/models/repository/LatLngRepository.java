package com.looksee.models.repository;

import com.looksee.models.LatLng;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@link LatLng} entity.
 */
@Repository
@Retry(name = "neoforj")
public interface LatLngRepository extends Neo4jRepository<LatLng, Long> {
	
	/**
	 * Finds a {@link LatLng} by its key.
	 * 
	 * @param key the key of the {@link LatLng} to find
	 * @return the {@link LatLng} with the given key
	 */
	@Query("MATCH (e:LatLng{key:$key}) RETURN e LIMIT 1")
	public LatLng findByKey(@Param("key") String key);
}
