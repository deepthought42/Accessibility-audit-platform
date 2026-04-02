package com.looksee.pageBuilder.models;

public class XYZColorSpace {
	double K = 903.3;
	double E = 0.008856;
	
	double x;
	double y;
	double z;
	
	public XYZColorSpace(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public CIEColorSpace XYZtoCIE() {
		
		double fx = calculateF(this.x);
		double fy = calculateF(this.y);
		double fz = calculateF(this.z);
		
		double l = 116*fy-16;;
		double a = 500*(fx-fy);
		double b = 200*(fy-fz);
		
		return new CIEColorSpace(l, a, b);
		
	}

	private double calculateF(double x) {
		if( x > E) {
			return Math.cbrt(x);
		}
		
		return ( K*x + 16 ) / 116;
		
	}
}
