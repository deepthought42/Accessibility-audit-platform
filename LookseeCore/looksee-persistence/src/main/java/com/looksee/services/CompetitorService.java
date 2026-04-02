package com.looksee.services;

import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.competitiveanalysis.brand.Brand;
import com.looksee.models.repository.CompetitorRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class CompetitorService {

	@Autowired
	private CompetitorRepository competitor_repo;

	/**
	 * Saves a {@link Competitor}
	 *
	 * @param competitor the {@link Competitor} to save
	 * @return the saved {@link Competitor}
	 *
	 * precondition: competitor != null
	 */
	public Competitor save(Competitor competitor) {
		assert competitor != null;

		return competitor_repo.save(competitor);
	}

	/**
	 * Deletes a {@link Competitor}
	 *
	 * @param competitor the {@link Competitor} to delete
	 *
	 * precondition: competitor != null
	 */
	public void delete(Competitor competitor) {
		assert competitor != null;

		competitor_repo.delete(competitor);
	}

	/**
	 * Deletes a {@link Competitor} by id
	 *
	 * @param competitor_id the id of the {@link Competitor} to delete
	 *
	 * precondition: competitor_id > 0
	 */
	public void deleteById(long competitor_id) {
		assert competitor_id > 0;

		competitor_repo.deleteById(competitor_id);
	}

	/**
	 * Retrieves all {@link Competitor}s
	 * 
	 * @return an iterable of all {@link Competitor}s
	 */
	public Iterable<Competitor> getAll() {
		return competitor_repo.findAll();
	}

	/**
	 * Adds a {@link Brand} to a {@link Competitor}
	 *
	 * @param competitor_id the id of the {@link Competitor}
	 * @param brand_id the id of the {@link Brand}
	 *
	 * precondition: competitor_id > 0
	 * precondition: brand_id > 0
	 */
	public void addBrand(long competitor_id, long brand_id) {
		assert competitor_id > 0;
		assert brand_id > 0;

		competitor_repo.addBrand(competitor_id, brand_id);
	}

	/**
	 * Finds a {@link Competitor} by id
	 *
	 * @param competitor_id the id of the {@link Competitor}
	 * @return an optional {@link Competitor}
	 *
	 * precondition: competitor_id > 0
	 */
	public Optional<Competitor> findById(long competitor_id) {
		assert competitor_id > 0;

		return competitor_repo.findById(competitor_id);
	}

	/**
	 * Checks if competitor has a competitive analysis currently in progress
	 * 
	 * @param brand {@link Brand} to check if analysis is running for
	 * @return true if analysis is running for the brand, false otherwise
	 * 
	 * precondition: brand != null
	 */
	public boolean isAnalysisRunning(Brand brand) {
		assert brand != null;

		if(brand != null) {
			Instant created = brand.getCreatedAt().toInstant(ZoneOffset.UTC);
			Instant now = Instant.now();
			Instant fifteen_minutes_ago = Instant.now().minusSeconds( (60*15) );
			return ( ! created.isBefore( fifteen_minutes_ago ) )  // "Not before" means "Is equal to or later".
				    			&& created.isBefore( now );
		}
	
		return false;
	}

	/**
	 * Retrieves the most recent {@link Brand} for a {@link Competitor}
	 *
	 * @param competitor_id the id of the {@link Competitor}
	 * @return the most recent {@link Brand}
	 *
	 * precondition: competitor_id > 0
	 */
	public Brand getMostRecentBrand(long competitor_id) {
		assert competitor_id > 0;

		return competitor_repo.getMostRecentBrand(competitor_id);
	}
}
