package com.crawlerApi.service;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.exceptions.AuditCreationException;
import com.looksee.gcp.PubSubUrlMessagePublisherImpl;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditStartMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.utils.BrowserUtils;

@Service
public class AuditService {
    
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    
    private final AccountService accountService;
    private final AuditRecordService auditRecordService;
    private final DomainService domainService;
    private final PubSubUrlMessagePublisherImpl urlTopic;
    private final JsonMapper jsonMapper;
    
    @Autowired
    public AuditService(
            AccountService accountService,
            AuditRecordService auditRecordService,
            DomainService domainService,
            PubSubUrlMessagePublisherImpl urlTopic) {
        this.accountService = accountService;
        this.auditRecordService = auditRecordService;
        this.domainService = domainService;
        this.urlTopic = urlTopic;
        this.jsonMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    }
    
    /**
     * Start an audit for a given URL and account
     * @param url The URL to audit
     * @param auditLevel The level of audit (PAGE or DOMAIN)
     * @param account The account requesting the audit
     * @return AuditRecordDto containing the audit details
     * @throws AuditCreationException if audit creation fails
     */
    public AuditRecordDto startAudit(String url, AuditLevel auditLevel, Account account) throws AuditCreationException {
        try {
            URL sanitizedUrl = sanitizeUrl(url);
            log.info("Starting {} audit for URL: {} for account: {}", auditLevel, sanitizedUrl, account.getId());
            
            switch (auditLevel) {
                case PAGE:
                    return startPageAudit(sanitizedUrl, account);
                case DOMAIN:
                    return startDomainAudit(sanitizedUrl, account);
                default:
                    throw new AuditCreationException("Unsupported audit level: " + auditLevel);
            }
        } catch (Exception e) {
            log.error("Failed to start audit for URL: {} and account: {}", url, account.getId(), e);
            throw new AuditCreationException("Failed to start audit: " + e.getMessage(), e);
        }
    }
    
    /**
     * Start a page-level audit
     */
    private AuditRecordDto startPageAudit(URL sanitizedUrl, Account account) throws Exception {
        log.info("Creating new page audit record for URL: {}", sanitizedUrl);
        
        PageAuditRecord auditRecord = createPageAuditRecord(sanitizedUrl);
        auditRecord = (PageAuditRecord) auditRecordService.save(auditRecord, null, null);
        
        // Associate audit with account
        accountService.addAuditRecord(account.getId(), auditRecord.getId());
        
        // Publish audit start message
        publishAuditStartMessage(sanitizedUrl, auditRecord.getId(), AuditLevel.PAGE, account.getId());
        
        log.info("Successfully created page audit with ID: {}", auditRecord.getId());
        return auditRecordService.buildAudit(auditRecord);
    }
    
    /**
     * Start a domain-level audit
     */
    private AuditRecordDto startDomainAudit(URL sanitizedUrl, Account account) throws Exception {
        log.info("Creating new domain audit record for URL: {}", sanitizedUrl);
        
        Domain domain = domainService.createDomain(sanitizedUrl, account.getId());
        
        // Create audit record
        Set<AuditName> auditList = getAvailableAuditList();
        AuditRecord auditRecord = new DomainAuditRecord(ExecutionStatus.RUNNING, auditList);
        auditRecord.setUrl(domain.getUrl());
        auditRecord = auditRecordService.save(auditRecord, account.getId(), domain.getId());
        
        // Associate audit with domain and account
        domainService.addAuditRecord(domain.getId(), auditRecord.getKey());
        accountService.addAuditRecord(account.getId(), auditRecord.getId());
        
        // Publish audit start message
        publishAuditStartMessage(sanitizedUrl, auditRecord.getId(), AuditLevel.DOMAIN, account.getId());
        
        log.info("Successfully created domain audit with ID: {}", auditRecord.getId());
        return auditRecordService.buildAudit(auditRecord);
    }
    
    /**
     * Create a new PageAuditRecord with default values
     */
    private PageAuditRecord createPageAuditRecord(URL sanitizedUrl) {
        PageAuditRecord auditRecord = new PageAuditRecord(
            ExecutionStatus.IN_PROGRESS,
            new HashSet<>(),
            null,
            false,
            new HashSet<>()
        );
        
        auditRecord.setUrl(sanitizedUrl.toString());
        auditRecord.setDataExtractionProgress(1.0 / 50.0);
        auditRecord.setAestheticScore(0.0);
        auditRecord.setAestheticAuditProgress(0.0);
        auditRecord.setContentAuditScore(0.0);
        auditRecord.setContentAuditProgress(0.0);
        auditRecord.setInfoArchScore(0.0);
        auditRecord.setInfoArchitectureAuditProgress(0.0);
        
        return auditRecord;
    }
    
    /**
     * Publish audit start message to the messaging system
     */
    private void publishAuditStartMessage(URL url, Long auditRecordId, AuditLevel auditLevel, Long accountId) throws Exception {
        AuditStartMessage auditStartMessage = new AuditStartMessage(
            url.toString(),
            BrowserType.CHROME,
            auditRecordId,
            auditLevel,
            accountId
        );
        
        String messageJson = jsonMapper.writeValueAsString(auditStartMessage);
        urlTopic.publish(messageJson);
        
        log.info("Published audit start message for audit ID: {}", auditRecordId);
    }
    
    /**
     * Sanitize and validate the input URL
     */
    private URL sanitizeUrl(String url) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        String lowercaseUrl = url.toLowerCase();
        String sanitizedUrlString = BrowserUtils.sanitizeUserUrl(lowercaseUrl);
        return new URL(sanitizedUrlString);
    }
    
    /**
     * Get the list of available audits for the current plan
     */
    private Set<AuditName> getAvailableAuditList() {
        Set<AuditName> auditList = new HashSet<>();
        
        // Visual Design Audits
        auditList.add(AuditName.TEXT_BACKGROUND_CONTRAST);
        auditList.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
        
        // Info Architecture Audits
        auditList.add(AuditName.LINKS);
        auditList.add(AuditName.TITLES);
        auditList.add(AuditName.ENCRYPTED);
        auditList.add(AuditName.METADATA);
        
        // Content Audits
        auditList.add(AuditName.ALT_TEXT);
        auditList.add(AuditName.READING_COMPLEXITY);
        auditList.add(AuditName.PARAGRAPHING);
        auditList.add(AuditName.IMAGE_COPYRIGHT);
        auditList.add(AuditName.IMAGE_POLICY);
        
        return auditList;
    }
} 