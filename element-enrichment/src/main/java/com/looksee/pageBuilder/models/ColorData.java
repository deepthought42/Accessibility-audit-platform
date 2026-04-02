package com.looksee.pageBuilder.models;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an both rgb and hsb and luminosity values
 *
 */
public class ColorData extends LookseeObject{
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorData.class);

	private double usagePercent;
	private int red;
	private int green;
	private int blue;
	
	private double transparency;
	private double brightness;
	private double hue;
	private double saturation;
	private double luminosity;
	
	/**
	 * 
	 * @param color_string
	 * 
	 * @pre rgba_string != null
	 * @pre !rgba_string.isEmpty()
	 */
	public ColorData(String color_string) {
		assert color_string != null;
		assert !color_string.isEmpty();
		
		if(color_string.startsWith("#")) {
			Color color = Color.decode(color_string);
			this.red = color.getRed();
			this.green = color.getGreen();
			this.blue = color.getBlue();
		}
		else {
			//extract r,g,b,a from color_str
			String tmp_color_str = color_string.replace(")", "");
			tmp_color_str = tmp_color_str.replace("rgba(", "");
			tmp_color_str = tmp_color_str.replace("rgb(", "");
			tmp_color_str = tmp_color_str.replaceAll(" ", "");
			String[] rgba = tmp_color_str.split(",");
			
			this.red = (int)Float.parseFloat(rgba[0]);
			this.green = (int)Float.parseFloat(rgba[1]);
			this.blue = (int)Float.parseFloat(rgba[2]);
			if(rgba.length == 4) {
				setTransparency(Double.parseDouble(rgba[3]));
			}
			else {
				setTransparency(1);
			}
		}
		
		
		//convert rgb to hsl, store all as Color object
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		this.hue = Math.round(hsb[0]*360);
		this.saturation = Math.round(hsb[1]*100);
		this.brightness = Math.round(hsb[2]*100);
		
		this.setLuminosity(calculatePercievedLightness(red, green, blue));		
	}

	public ColorData(ColorUsageStat color_usage_stat) {
		assert color_usage_stat != null;
		
		this.red = (int)color_usage_stat.getRed();
		this.green = (int)color_usage_stat.getGreen();
		this.blue = (int)color_usage_stat.getBlue();
		setTransparency(Double.parseDouble("1.0"));
	
		//convert rgb to hsl, store all as Color object
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		this.hue = Math.round(hsb[0]*360);
		this.saturation = Math.round(hsb[1]*100);
		this.brightness = Math.round(hsb[2]*100);
		
		this.setLuminosity(calculatePercievedLightness(red, green, blue));		
		setUsagePercent(color_usage_stat.getPixelPercent());
	}


	public ColorData(int red, int green, int blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		
		setTransparency(Double.parseDouble("1.0"));

		//convert rgb to hsl, store all as Color object
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		this.hue = Math.round(hsb[0]*360);
		this.saturation = Math.round(hsb[1]*100);
		this.brightness = Math.round(hsb[2]*100);
		
		this.setLuminosity(calculatePercievedLightness(red, green, blue));		
	}

	/**
	 * Calculates luminosity from rgb color
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	private double calculatePercievedLightness(int red, int green, int blue) {
		//calculate luminosity
		//For the sRGB colorspace, the relative luminance of a color is defined as
		//where R, G and B are defined as:
		
		double RsRGB = red/255.0;
		double GsRGB = green/255.0;
		double BsRGB = blue/255.0;

		double R = sRGBtoLin(RsRGB);
		double G = sRGBtoLin(GsRGB);
		double B = sRGBtoLin(BsRGB);
		
		return 0.2126 * R + 0.7152 * G + 0.0722 * B ;
	}
	
	private double YtoLstar(double luminance) {
        // Send this function a luminance value between 0.0 and 1.0,
        // and it returns L* which is "perceptual lightness"
	
	    if ( luminance <= (216/24389.0) ) {       // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            return luminance * (24389/27.0);  // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            return Math.pow(luminance,(1/3.0)) * 116 - 16;
        }
	}
	
	private double sRGBtoLin(double colorChannel) {
        // Send this function a decimal sRGB gamma encoded color value
        // between 0.0 and 1.0, and it returns a linearized value.

	    if ( colorChannel <= 0.04045 ) {
            return colorChannel / 12.92;
        } else {
            return Math.pow((( colorChannel + 0.055)/1.055),2.4);
        }
    }
	
	/**
	 * Conver RGB to XYZ from rgb color
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	public XYZColorSpace RGBtoXYZ() {
		//calculate luminosity
		//For the sRGB colorspace, the relative luminance of a color is defined as
		//where R, G and B are defined as:
		double RsRGB = red/255.0;
		double GsRGB = green/255.0;
		double BsRGB = blue/255.0;
		double X = 0.412453*RsRGB + 0.357580 * RsRGB + 0.180423 * RsRGB;
		double Y = 0.212671*GsRGB + 0.715160 *GsRGB + 0.072169 * GsRGB; 
		double Z  = 0.019334*BsRGB  +  0.119193*BsRGB  + 0.950227 * BsRGB; 
	
		return new XYZColorSpace(X, Y, Z);
	}

	/**
	 * Returns a string with rgb values separated by a comma. For example 0,0,0
	 * 
	 * @return
	 */
	public String rgb() {
		return red+","+green+","+blue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
        if (!(obj instanceof ColorData)) return false;
        
		ColorData color = (ColorData)obj;
		return color.blue == this.blue && color.red == this.red && color.green == this.green;
	}
	
	/**
	 * Returns the RGB representation of this color
	 */
	@Override
	public String toString() {
		return rgb();
	}

	@Override
	public ColorData clone() {
		return new ColorData(this.red, this.green, this.blue);
	}
	
	
	public String hsb() {
		return hue+" , "+saturation+" , "+brightness;
	}

	public int getRed() {
		return red;
	}
	
	public int getGreen() {
		return green;
	}
	
	public int getBlue() {
		return blue;
	}
	
	public double getTransparency() {
		return transparency;
	}

	private void setTransparency(double transparency) {
		this.transparency = transparency;
	}

	public double getLuminosity() {
		return luminosity;
	}

	private void setLuminosity(double luminosity) {
		this.luminosity = luminosity;
	}
	

	public double getHue() {
		return hue;
	}

	public double getSaturation() {
		return saturation;
	}
	
	public double getBrightness() {
		return brightness;
	}

	/**
	 * Computes the contrast of the 2 colors provided
	 * 
	 * @param color_data_1
	 * @param color_data_2
	 * 
	 * @return contrast value
	 * 
	 * @pre color_data_1 != null
	 * @pre color_data_2 != null
	 */
	public static double computeContrast(ColorData color_data_1, ColorData color_data_2) {
		assert color_data_1 != null;
		assert color_data_2 != null;
		
		double max_luminosity = 0.0;
		double min_luminosity = 0.0;
		
		if(color_data_1.getLuminosity() > color_data_2.getLuminosity()) {
			min_luminosity = color_data_2.getLuminosity();
			max_luminosity = color_data_1.getLuminosity();
		}
		else {
			min_luminosity = color_data_1.getLuminosity();
			max_luminosity = color_data_2.getLuminosity();
		}
		return (max_luminosity + 0.05) / (min_luminosity + 0.05);
	}

	@Override
	public String generateKey() {
		return rgb();
	}

	public double getUsagePercent() {
		return usagePercent;
	}

	public void setUsagePercent(double usage_percent) {
		this.usagePercent = usage_percent;
	}

	public void alphaBlend(ColorData background_color_data) {
		this.red = (int) (((1 - getTransparency()) * background_color_data.getRed()) + (getTransparency() * getRed()));
		this.green = (int) (((1 - getTransparency()) * background_color_data.getGreen()) + (getTransparency() * getGreen()));
		this.blue = (int) (((1 - getTransparency()) * background_color_data.getBlue()) + (getTransparency() * getBlue()));
	
		//convert rgb to hsl, store all as Color object
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		this.hue = hsb[0];
		this.saturation = hsb[1];
		this.brightness = hsb[2];
		
		this.setLuminosity(calculatePercievedLightness(red, green, blue));
	}

	
	/**
	 * Checks if both colors are within a given distance of each other on the conic HSL(Hugh, Saturation, Luminosity) color representation
	 * 
	 * @param palette_color color of primary color being evaluated against
	 * @param color {@link ColorData} color being evaluated
	 * 
	 * @return true if color2 is within 5 arc degress of color1
	 */
	public boolean isSimilarHue(ColorData color2) {		
		return Math.abs(this.getHue() - color2.getHue()) <= 5.0;
	}

	public String getHex() {
		String red_value = Integer.toHexString(getRed());
		if(red_value.length() == 1) {
			red_value = "0"+red_value;
		}
		
		String green_value = Integer.toHexString(getGreen());
		if(green_value.length() == 1) {
			green_value = "0"+green_value;
		}
		
		String blue_value = Integer.toHexString(getBlue());
		if(blue_value.length() == 1) {
			blue_value = "0"+blue_value;
		}
		
		String hex_value = "#"+red_value+green_value+blue_value;
		log.warn("Hex value :: "+hex_value);
		return hex_value;
	}
}