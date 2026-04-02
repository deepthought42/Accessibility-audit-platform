package com.looksee.models.repository;

import com.looksee.models.ImageFaceAnnotation;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ImageFaceAnnotation}s
 */
@Repository
@Retry(name = "neoforj")
public interface ImageFaceAnnotationRepository extends Neo4jRepository<ImageFaceAnnotation, Long> {
	
	/**
	 * Finds an {@link ImageFaceAnnotation} by its key
	 *
	 * @param key the key
	 * @return the image face annotation
	 */
	@Query("MATCH (e:ImageFaceAnnotation{key:$key}) RETURN e LIMIT 1")
	public ImageFaceAnnotation findByKey(@Param("key") String key);
}
