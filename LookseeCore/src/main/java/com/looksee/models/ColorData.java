package com.looksee.models;

import java.awt.Color;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an both rgb and hsb and luminosity values.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: red is between 0 and 255 inclusive</li>
 *   <li>invariant: green is between 0 and 255 inclusive</li>
 *   <li>invariant: blue is between 0 and 255 inclusive</li>
 *   <li>invariant: hue is between 0 and 360 inclusive</li>
 *   <li>invariant: saturation is between 0 and 100 inclusive</li>
 *   <li>invariant: brightness is between 0 and 100 inclusive</li>
 * </ul>
 */
public class ColorData extends LookseeObject{
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorData.class);

	@Getter
	@Setter
	private double usagePercent;

	@Getter
	private int red;

	@Getter
	private int green;

	@Getter
	private int blue;

	@Getter
	@Setter
	private double transparency;

	@Getter
	private double brightness;

	@Getter
	private double hue;

	@Getter
	private double saturation;

	@Getter
	@Setter
	private double luminosity;
	
	/**
	 * Constructs a new {@link ColorData}
	 *
	 * @param color_string the color string
	 *
	 * precondition: rgba_string != null
	 * precondition: !rgba_string.isEmpty()
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

	/**
	 * Constructs a new {@link ColorData}
	 *
	 * @param color_usage_stat the color usage stat
	 *
	 * precondition: color_usage_stat != null
	 */
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

	/**
	 * Constructs a new {@link ColorData}
	 *
	 * @param red the red value of the color
	 * @param green the green value of the color
	 * @param blue the blue value of the color
	 */
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
	 * @param red the red value of the color
	 * @param green the green value of the color
	 * @param blue the blue value of the color
	 * @return the luminosity of the color
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
	
	/**
	 * Converts Y to L*
	 *
	 * @param luminance the luminance value
	 * @return the L* value
	 */
	public static double YtoLstar(double luminance) {
        // Send this function a luminance value between 0.0 and 1.0,
        // and it returns L* which is "perceptual lightness"
	
	    if ( luminance <= (216/24389.0) ) {       // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            return luminance * (24389/27.0);  // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            return Math.pow(luminance,(1/3.0)) * 116 - 16;
        }
	}
	
	/**
	 * Converts sRGB to linear
	 *
	 * @param colorChannel the color channel
	 * @return the linear value
	 */
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
	 * Converts RGB to XYZ from rgb color
	 *
	 * @return the {@link XYZColorSpace}
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
	 * @return the rgb values separated by a comma
	 */
	public String rgb() {
		return red+","+green+","+blue;
	}
	
	/**
	 * Checks if the color is equal to another color
	 *
	 * @param obj the object to check
	 * @return true if the color is equal to the other color, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
        if (!(obj instanceof ColorData)) return false;
        
		ColorData color = (ColorData)obj;
		return color.blue == this.blue && color.red == this.red && color.green == this.green;
	}
	
	/**
	 * Returns the RGB representation of this color
	 *
	 * @return the rgb values separated by a comma
	 */
	@Override
	public String toString() {
		return rgb();
	}

	/**
	 * Clones the color
	 *
	 * @return the cloned color
	 */
	@Override
	public ColorData clone() {
		return new ColorData(this.red, this.green, this.blue);
	}
	
	
	/**
	 * Returns the HSB representation of the color
	 *
	 * @return the hsb values separated by a comma
	 */
	public String hsb() {
		return hue+" , "+saturation+" , "+brightness;
	}
	
	/**
	 * Computes the contrast of the 2 colors provided
	 * 
	 * @param color_data_1 the first color data
	 * @param color_data_2 the second color data
	 * 
	 * @return the contrast value
	 * 
	 * precondition: color_data_1 != null
	 * precondition: color_data_2 != null
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

	/**
	 * Generates a key for the color
	 *
	 * @return the key
	 */
	@Override
	public String generateKey() {
		return rgb();
	}

	/**
	 * Alpha blends the color with a background color
	 *
	 * @param background_color_data the background color data
	 *
	 * precondition: background_color_data != null
	 */
	public void alphaBlend(ColorData background_color_data) {
		assert background_color_data != null;
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
	 * Checks if both colors are within a given distance of each other on the
	 * conic HSL(Hugh, Saturation, Luminosity) color representation
	 *
	 * @param color the {@link ColorData} color being evaluated
	 *
	 * @return true if the colors are within 5 arc degress of each other, false otherwise
	 *
	 * precondition: color != null
	 */
	public boolean isSimilarHue(ColorData color) {
		assert color != null;

		return Math.abs(this.getHue() - color.getHue()) <= 5.0;
	}

	/**
	 * Returns the hex value of the color
	 *
	 * @return the hex value of the color
	 */
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
		return hex_value;
	}
}