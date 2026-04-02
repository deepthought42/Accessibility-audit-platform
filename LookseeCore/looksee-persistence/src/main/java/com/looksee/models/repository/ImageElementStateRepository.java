package com.looksee.models.repository;

import com.looksee.models.ImageElementState;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ImageElementState}s
 */
@Repository
@Retry(name = "neoforj")
public interface ImageElementStateRepository extends Neo4jRepository<ImageElementState, Long> {
	
	/**
	 * Finds an {@link ImageElementState} by its key
	 *
	 * @param key the key
	 * @return the {@link ImageElementState}
	 */
	@Query("MATCH (e:ImageElementState{key:$key}) RETURN e LIMIT 1")
	public ImageElementState findByKey(@Param("key") String key);
}
