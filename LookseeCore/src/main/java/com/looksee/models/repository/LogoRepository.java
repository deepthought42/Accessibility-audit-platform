package com.looksee.models.repository;

import com.looksee.models.Logo;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@link Logo} entity.
 */
@Repository
@Retry(name = "neoforj")
public interface LogoRepository extends Neo4jRepository<Logo, Long> {
	
	/**
	 * Finds a logo by its key.
	 * 
	 * @param key the key of the logo to find
	 * @return the logo with the given key
	 */
	@Query("MATCH (Logo{key:$key}) RETURN e LIMIT 1")
	public Logo findByKey(@Param("key") String key);
}
