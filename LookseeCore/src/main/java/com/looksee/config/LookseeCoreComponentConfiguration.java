package com.looksee.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class responsible for component scanning of LookseeCore services and components.
 * 
 * This configuration enables discovery and registration of all @Service, @Component, and other
 * Spring-managed beans in the LookseeCore packages, making them available to consuming applications.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.looksee.designsystem",
    "com.looksee.exceptions",
    "com.looksee.models",
    "com.looksee.models.domain",
    "com.looksee.models.audit",
    "com.looksee.models.audit.performance",
    "com.looksee.models.audit.aesthetics",
    "com.looksee.models.audit.content",
    "com.looksee.models.audit.domain",
    "com.looksee.models.audit.recommend",
    "com.looksee.models.audit.informationarchitecture",
    "com.looksee.models.competitiveanalysis",
    "com.looksee.models.competitiveanalysis.brand",
    "com.looksee.models.journeys",
    "com.looksee.models.rules",
    "com.looksee.services",
    "com.looksee.gcp",
    "com.looksee.integrations",
    "com.looksee.browsing",
    "com.looksee.browsing.form",
    "com.looksee.browsing.helpers",
    "com.looksee.browsing.table",
    "com.looksee.mapper",
    "com.looksee.utils",
    "com.looksee.vscodePlugin.structs",
    "com.looksee.vscodePlugin.structs.dto"
})
public class LookseeCoreComponentConfiguration {
    
    /**
     * This configuration class is responsible for scanning and registering
     * all LookseeCore components as Spring beans.
     * 
     * The component scanning includes:
     * - Services (@Service)
     * - Components (@Component)  
     * - Model classes with Spring annotations
     * - Utility classes
     * - Integration services
     * - Browser automation components
     * - GCP services
     * - Mapper classes
     * - VSCode plugin structures
     * 
     * Note: Repository configuration is handled separately in LookseeCoreRepositoryConfiguration
     * to maintain proper separation of concerns and avoid circular dependencies.
     */
} 