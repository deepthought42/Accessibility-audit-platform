package com.looksee.services;

import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.repository.DesignSystemRepository;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing design systems
 */
@NoArgsConstructor
@Service
public class DesignSystemService {

	@Autowired
	private DesignSystemRepository design_system_repo;

	/**
	 * Save a design system
	 *
	 * @param design_system the design system to save
	 * @return the saved design system
	 *
	 * precondition: design_system != null
	 */
	public DesignSystem save(DesignSystem design_system) {
		assert design_system != null;
		return design_system_repo.save(design_system);
	}

	/**
	 * Find a design system by id
	 *
	 * @param id the id of the design system
	 * @return the design system
	 *
	 * precondition: id > 0
	 */
	public Optional<DesignSystem> findById(long id) {
		assert id > 0;
		return design_system_repo.findById(id);
	}
}
