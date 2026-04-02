package com.looksee.models.competitiveanalysis.brand;

import com.looksee.models.LookseeObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a brand
 */
@Getter
@Setter
public class Brand extends LookseeObject {
	private List<String> colors;
	
	/**
	 * Constructor for {@link Brand}
	 */
	public Brand() {
		super();
		setColors(new ArrayList<String>());
	}
	
	/**
	 * Constructor for {@link Brand}
	 *
	 * @param colors the colors of the brand
	 *
	 * precondition: colors != null
	 */
	public Brand( List<String> colors ) {
		super();

		assert colors != null;

		setColors(colors);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "brand"+UUID.randomUUID();
	}
}
