package com.looksee.models.repository;

import com.looksee.models.competitiveanalysis.brand.Brand;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Brand} objects
 */
@Repository
@Retry(name = "neoforj")
public interface BrandRepository extends Neo4jRepository<Brand, Long> {
	/**
	 * Finds a {@link Brand} by its key
	 *
	 * @param key the key of the brand
	 * @return the brand
	 */
	public Brand findByKey(@Param("key") String key);
}
