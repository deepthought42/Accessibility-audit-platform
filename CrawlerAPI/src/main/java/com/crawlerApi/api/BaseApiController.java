package com.crawlerApi.api;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.services.AccountService;


@Validated
public abstract class BaseApiController {
    
    protected static final Logger log = LoggerFactory.getLogger(BaseApiController.class);
    
    @Autowired
    protected AccountService accountService;
    
    /**
     * Get authenticated account from principal with proper error handling
     */
    protected Account getAuthenticatedAccount(Principal principal) throws UnknownAccountException {
        String userId = principal.getName().replace("auth0|", "");
        Account account = accountService.findByUserId(userId);
        if (account == null) {
            throw new UnknownAccountException();
        }
        return account;
    }
    
    /**
     * Build pagination from request parameters
     */
    protected Pageable buildPageable(int page, int size, String sort) {
        // Validate and sanitize pagination parameters
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100); // Cap at 100 items
        
        Sort sortSpec = buildSort(sort);
        return PageRequest.of(page, size, sortSpec);
    }
    
    /**
     * Parse sort parameter like "createdAt,desc" or "name"
     */
    protected Sort buildSort(String sortParam) {
        if (StringUtils.isEmpty(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
            
        return Sort.by(direction, property);
    }
}