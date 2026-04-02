package com.crawlerApi.api;

import java.net.MalformedURLException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.TestUser;
import com.looksee.models.repository.TestUserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller that defines endpoints to access test users
 */
@Controller
@RequestMapping(path = "v1/testusers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Test Users V1", description = "Test Users API")
public class TestUserController extends BaseApiController {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestUserController.class);

	@Autowired
	private TestUserRepository test_user_repo;
	
	/**
	 * Create a new test user and add it to the domain
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
    @PreAuthorize("hasAuthority('create:test_user')")
    @RequestMapping(path="$user_id", method = RequestMethod.PUT)
    @Operation(summary = "Update test user", description = "Update a test user with the given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated test user"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Test user not found")
    })
    public @ResponseBody void updateUser(HttpServletRequest request,
    									@PathVariable(value="user_id", required=true) long user_id,
    									@RequestParam(value="isEnabled", required=true) boolean isEnabled,
    									@RequestParam(value="password", required=true) String password,
    									@RequestParam(value="username", required=true) String username,
    									@RequestParam(value="role", required=false) String role)  {
    	Optional<TestUser> optional_user = test_user_repo.findById(user_id);
    	if(optional_user.isPresent()){
    		TestUser test_user_record = optional_user.get();
    		    		
    		test_user_record.setPassword(password);
    		test_user_record.setUsername(username);
    		test_user_repo.save(test_user_record);
    	}
    	else{
    		throw new TestUserNotFoundException();
    	}
    }
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class TestUserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public TestUserNotFoundException() {
		super("Test user could not be found.");
	}
}

