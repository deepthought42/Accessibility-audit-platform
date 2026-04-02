package com.crawlerApi.integrations.provider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationProvider;

/**
 * Product Board integration. Used for POST /v1/integrations/product-board JWT creation (backward compatibility).
 */
@Component
public class ProductBoardIntegrationProvider implements IntegrationProvider {

    private static final List<String> CONFIG_KEYS = Collections.emptyList();

    @Override
    public String getType() {
        return "product-board";
    }

    @Override
    public IntegrationMetadata getMetadata() {
        return new IntegrationMetadata("product-board", "Product Board", "Product Management", CONFIG_KEYS);
    }

    @Override
    public boolean validateConfig(Map<String, Object> config) {
        return true;
    }
}
