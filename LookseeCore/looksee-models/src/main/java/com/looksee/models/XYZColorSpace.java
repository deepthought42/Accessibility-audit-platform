package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a XYZ color space
 */
@Getter
@Setter
@NoArgsConstructor
public class XYZColorSpace {

	/**
	 * The K value of the XYZ color space
	 */
	double K = 903.3;

	/**
	 * The E value of the XYZ color space
	 */
	double E = 0.008856;
	
	/**
	 * The x value of the XYZ color space
	 */
	double x;
	
	/**
	 * The y value of the XYZ color space
	 */
	double y;
	
	/**
	 * The z value of the XYZ color space
	 */
	double z;
	
	/**
	 * Constructs a new {@link XYZColorSpace}
	 *
	 * @param x the x value of the XYZ color space
	 * @param y the y value of the XYZ color space
	 * @param z the z value of the XYZ color space
	 */
	public XYZColorSpace(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Converts the XYZ color space to a CIE color space
	 *
	 * @return the CIE color space
	 */
	public CIEColorSpace XYZtoCIE() {
		
		double fx = calculateF(this.x);
		double fy = calculateF(this.y);
		double fz = calculateF(this.z);
		
		double l = 116*fy-16;;
		double a = 500*(fx-fy);
		double b = 200*(fy-fz);
		
		return new CIEColorSpace(l, a, b);
		
	}

	/**
	 * Calculates the f value of the XYZ color space
	 *
	 * @param x the x value of the XYZ color space
	 * @return the f value of the XYZ color space
	 */
	private double calculateF(double x) {
		if( x > E) {
			return Math.cbrt(x);
		}
		
		return ( K*x + 16 ) / 116;
		
	}
}
