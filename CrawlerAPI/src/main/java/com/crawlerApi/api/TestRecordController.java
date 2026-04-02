package com.crawlerApi.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.looksee.exceptions.DomainNotOwnedByAccountException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Test;
import com.looksee.models.TestRecord;
import com.looksee.models.repository.TestRecordRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@Controller
@RequestMapping(path = "v1/testrecords", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Test Records V1", description = "Test Records API")
public class TestRecordController extends BaseApiController {

	@Autowired
	private TestRecordRepository test_record_repo;
	
	/**
 	 * Retrieves list of all test records for a test from the database
 	 * 
	 * @param url
	 * @return
	 */
    @PreAuthorize("hasAuthority('read:test_records')")
	@RequestMapping(method = RequestMethod.GET)
	@Operation(summary = "Get all test records", description = "Retrieve list of all test records")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved test records", content = @Content(schema = @Schema(type = "array", implementation = TestRecord.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody List<TestRecord> getTestRecords(@RequestParam(value="url", required=true) Test test) {
		return IterableUtils.toList(test_record_repo.findAll());
	}

    
    /**
	 * Retrieves list of all tests from the database
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 * @throws DomainNotOwnedByAccountException 
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('read:test_records')")
	@RequestMapping(path="$key", method = RequestMethod.GET)
	@Operation(summary = "Get test record by key", description = "Retrieve test record with the given key")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved test record", content = @Content(schema = @Schema(type = "object", implementation = TestRecord.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions"),
		@ApiResponse(responseCode = "404", description = "Test record not found")
	})
	public @ResponseBody TestRecord getTestRecord(HttpServletRequest request, 
														@PathVariable(value="key", required=true) String key) {
    	return test_record_repo.findByKey(key);
	}
}
