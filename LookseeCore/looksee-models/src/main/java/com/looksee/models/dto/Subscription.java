package com.looksee.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subscription object for a user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
	private String plan;
	private String priceId;
}
