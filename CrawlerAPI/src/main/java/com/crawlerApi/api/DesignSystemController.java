package com.crawlerApi.api;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.looksee.browsing.Crawler;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DesignSystemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "v1/designsystem", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Design System V1", description = "Design System API")
public class DesignSystemController extends BaseApiController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected AuditService audit_service;
	
	@Autowired
	private DesignSystemService design_system_service;
	
	/**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/{id}/color", method = RequestMethod.POST)
	@Operation(summary = "Update design system colors", description = "Update the color palette for the design system")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully updated color palette"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Design system not found")
	})
	public @ResponseBody void updateColors(
			HttpServletRequest request,
			@PathVariable(value="id", required=true) long id,
			@RequestBody(required=true) List<String> colors
	) throws Exception {
		Optional<DesignSystem> design_system_opt = design_system_service.findById(id);
		if(design_system_opt.isPresent()){
			DesignSystem design_system = design_system_opt.get();
			design_system.setColorPalette(colors);
			design_system_service.save(design_system);
		}
	}
}