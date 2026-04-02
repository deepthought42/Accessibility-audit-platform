package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all subscription plans that exist in the system
 */
public enum SubscriptionPlan {
    /**
     * The free subscription plan
     */
    FREE("FREE"),
    /**
     * The enterprise subscription plan
     */
    ENTERPRISE("ENTERPRISE"),
    /**
     * The startup subscription plan
     */
    STARTUP("STARTUP"),
    /**
     * The company pro subscription plan
     */
    COMPANY_PRO("PRO"),
    /**
     * The company premium subscription plan
     */
    COMPANY_PREMIUM("PREMIUM"),
    /**
     * The agency subscription plan
     */
	AGENCY_PRO("AGENCY_PRO"),
    
    /**
     * The agency premium subscription plan
     */
	AGENCY_PREMIUM("AGENCY_PREMIUM"),
    /**
     * The unlimited subscription plan
     */
    UNLIMITED("UNLIMITED");

    private final String short_name;

    /**
     * Constructor for {@link SubscriptionPlan}
     *
     * @param name the name of the subscription plan
     */
    private SubscriptionPlan(final String name) {
        this.short_name = name;
    }

    /**
     * Returns the short name of the subscription plan
     *
     * @return the short name of the subscription plan
     */
    @Override
    public String toString() {
        return short_name;
    }
    
    /**
     * Creates a {@link SubscriptionPlan} from a string value
     *
     * @param value the value to create the subscription plan from
     * @return the subscription plan enum object
     *
     * precondition: value != null
     */
    @JsonCreator
    public static SubscriptionPlan create (String value) {
        assert value != null;
        
        for(SubscriptionPlan v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }
    
    /**
     * Returns the short name of the subscription plan
     *
     * @return the short name of the subscription plan
     */
    public String getShortName() {
        return short_name;
    }
}
