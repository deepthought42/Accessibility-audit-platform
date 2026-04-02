package com.looksee.models;

/**
 * Represents a CIE color space
 */
public class CIEColorSpace {
	/**
	 * The lightness of the color
	 */
	public double l;
	/**
	 * The a value of the color
	 */
	public double a;
	/**
	 * The b value of the color
	 */
	public double b;
	
	/**
	 * Constructs a new {@link CIEColorSpace}
	 *
	 * @param l the lightness of the color
	 * @param a the a value of the color
	 * @param b the b value of the color
	 */
	public CIEColorSpace(double l, double a, double b) {
		this.l = l;
		this.a = a;
		this.b = b;
	}
}
