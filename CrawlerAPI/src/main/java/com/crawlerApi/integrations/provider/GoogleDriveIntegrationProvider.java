package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Google Drive integration (Product Management). Stub implementation.
 */
@Component
public class GoogleDriveIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("clientId", "clientSecret", "folderIds");

    public GoogleDriveIntegrationProvider() {
        super("google-drive", "Google Drive", "Product Management", CONFIG_KEYS);
    }
}
