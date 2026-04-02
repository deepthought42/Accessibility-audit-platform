package com.looksee.models.repository;

import com.looksee.models.ImageLandmarkInfo;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ImageLandmarkInfo}s
 */
@Repository
@Retry(name = "neoforj")
public interface ImageLandmarkInfoRepository extends Neo4jRepository<ImageLandmarkInfo, Long> {
	
	/**
	 * Finds an {@link ImageLandmarkInfo} by its key
	 *
	 * @param key the key
	 * @return the image landmark info
	 */
	@Query("MATCH (e:ImageLandmarkInfo{key:$key}) RETURN e LIMIT 1")
	public ImageLandmarkInfo findByKey(@Param("key") String key);
}
