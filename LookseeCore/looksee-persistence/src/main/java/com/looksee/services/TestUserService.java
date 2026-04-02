package com.looksee.services;

import com.looksee.models.TestUser;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.repository.TestUserRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Enables interacting with database for {@link SimpleStep Steps}
 */
@Service
@Retry(name = "neoforj")
public class TestUserService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestUserService.class);

	@Autowired
	private TestUserRepository test_user_repo;
	
	/**
	 * Finds a {@link TestUser} by their ID
	 *
	 * @param id the ID of the {@link TestUser} to find
	 *
	 * @return the {@link TestUser} with the given ID
	 *
	 * precondition: id > 0
	 */
	public TestUser findById(long id) {
		assert id > 0;

		return test_user_repo.findById(id).get();
	}

	/**
	 * Saves a {@link TestUser} to the database
	 *
	 * @param user the {@link TestUser} to save
	 *
	 * @return the saved {@link TestUser}
	 */
	public TestUser save(TestUser user) {
		assert user != null;
		
		return test_user_repo.save(user);
	}
}
