package com.looksee.frontEndBroadcaster.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.frontEndBroadcaster.models.TestUser;
import com.looksee.frontEndBroadcaster.models.repository.TestUserRepository;

import io.github.resilience4j.retry.annotation.Retry;

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
	
	public TestUser findById(long id) {
		return test_user_repo.findById(id).get();
	}

	public TestUser save(TestUser user) {
		assert user != null;
		
		return test_user_repo.save(user);
	}
}
