package com.looksee.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.gcp.GoogleCloudStorage;
import com.looksee.pageBuilder.models.ColorData;
import com.looksee.pageBuilder.models.ColorUsageStat;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.BrowserType;

import io.github.resilience4j.retry.annotation.Retry;

public class ImageUtils {
	private static Logger log = LoggerFactory.getLogger(ImageUtils.class);

	 public static BufferedImage resize(BufferedImage img, int height, int width) {
		 /* 
		 BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
		 BufferedImage scaledImage = resampler.filter(img, null);
		 return scaledImage;
		  */
		Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, img.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
	 
	public static int[] rgb2lab(int R, int G, int B) {
	    //http://www.brucelindbloom.com

	    float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
	    float Ls, as, bs;
	    float eps = 216.f/24389.f;
	    float k = 24389.f/27.f;

	    float Xr = 0.964221f;  // reference white D50
	    float Yr = 1.0f;
	    float Zr = 0.825211f;

	    // RGB to XYZ
	    r = R/255.f; //R 0..1
	    g = G/255.f; //G 0..1
	    b = B/255.f; //B 0..1

	    // assuming sRGB (D65)
	    if (r <= 0.04045)
	        r = r/12;
	    else
	        r = (float) Math.pow((r+0.055)/1.055,2.4);

	    if (g <= 0.04045)
	        g = g/12;
	    else
	        g = (float) Math.pow((g+0.055)/1.055,2.4);

	    if (b <= 0.04045)
	        b = b/12;
	    else
	        b = (float) Math.pow((b+0.055)/1.055,2.4);


	    X =  0.436052025f*r     + 0.385081593f*g + 0.143087414f *b;
	    Y =  0.222491598f*r     + 0.71688606f *g + 0.060621486f *b;
	    Z =  0.013929122f*r     + 0.097097002f*g + 0.71418547f  *b;

	    // XYZ to Lab
	    xr = X/Xr;
	    yr = Y/Yr;
	    zr = Z/Zr;

	    if ( xr > eps )
	        fx =  (float) Math.pow(xr, 1/3.0);
	    else
	        fx = (float) ((k * xr + 16.0) / 116.0);

	    if ( yr > eps )
	        fy =  (float) Math.pow(yr, 1/3.0);
	    else
	    fy = (float) ((k * yr + 16.0) / 116.0);

	    if ( zr > eps )
	        fz =  (float) Math.pow(zr, 1/3.0);
	    else
	        fz = (float) ((k * zr + 16.0) / 116);

	    Ls = ( 116 * fy ) - 16;
	    as = 500*(fx-fy);
	    bs = 200*(fy-fz);
	    int[] lab = new int[3];
	    lab[0] = (int) (2.55*Ls + 0.5);
	    lab[1] = (int) (as + 0.5); 
	    lab[2] = (int) (bs + 0.5);       
	    
	    return lab;
	}

	/**
	 * Measures color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static List<ColorUsageStat> extractImageProperties(BufferedImage buffered_image) throws IOException {
		int width = buffered_image.getWidth();
		int height = buffered_image.getHeight();		
		
		int desired_width = 768;
		int desired_height = 1024;
		

		//scale down
		double w_scale = Math.ceil(desired_width / (double)width);
		double h_scale = Math.ceil(desired_height / (double)height);
		
		if(h_scale > w_scale) {
			desired_width = (int)(h_scale * width);
			desired_height = (int)(h_scale * height);
		}
		else {
			desired_width = (int)(w_scale * width);
			desired_height = (int)(w_scale * height);
		}
		
		//buffered_image = ImageUtils.resize(buffered_image, desired_width, desired_height);
		//return CloudVisionUtils.extractImageProperties(buffered_image);
		
		
		List<ColorUsageStat> color_usage_stats = new ArrayList<>();
		width = buffered_image.getWidth();
		height = buffered_image.getHeight();	
		
		Map<String, Integer> colors = new HashMap<>();
		//extract colors using a random sample of image pixels
		int sample_size = (int)( ( width * height) * 0.10 );
		
		for(int sample_idx=0; sample_idx < sample_size; sample_idx++) {
			int x = getRandomNumberUsingNextInt(0, width-1);
			int y = getRandomNumberUsingNextInt(0, height-1);
			
			String rgb = getPixelColor(buffered_image, x, y);
	        if(colors.containsKey(rgb)) {
	        	colors.put(rgb, colors.get(rgb)+1);	
	        }else {
	        	colors.put(rgb, 1);
	        }
		}
		
		
		// Getting pixel color by position x and y
		/*
		 * NOTE : ORIGINAL WORKING CODE. REMOVE IF STILL HERE AFTER 3/1/2022
		for(int x=0; x < width; x+=3) {
			for(int y=0; y < height; y+=3) {
				int clr = buffered_image.getRGB(x, y);
		        int red =   (clr & 0x00ff0000) >> 16;
		        int green = (clr & 0x0000ff00) >> 8;
		        int blue =   clr & 0x000000ff;
		        String rgb = red+","+green+","+blue;
		        if(colors.containsKey(rgb)) {
		        	colors.put(rgb, colors.get(rgb)+1);	
		        }else {
		        	colors.put(rgb, 1);
		        }
			}
		}
		*/
       
		for(String color_str: colors.keySet()) {
			ColorData color = new ColorData(color_str);
			double percent = ((double)colors.get(color_str)) / ((double) ( width * height ));
			//log.warn(color_str+"     :     "+percent);
			ColorUsageStat color_stat = new ColorUsageStat(color.getRed(), color.getGreen(), color.getBlue(), percent, 0);
			color_usage_stats.add(color_stat);
		}
	    return color_usage_stats;
	    
	}

	/**
	 * Retrieves the color for a given pixel and return the rgb value as a comma separated string
	 * ( ie. 255, 255, 255 ) 
	 * 
	 * @param img
	 * @param x coordinate for x axis
	 * @param y coordinate for y axis
	 * 
	 * @pre img != null
	 * 
	 * @return rgb value as a comma separated string ( ie. 255, 255, 255 ) 
	 */
	private static String getPixelColor(BufferedImage img, int x, int y) {
		assert img != null;

		int clr = img.getRGB(x, y);
        int red =   (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue =   clr & 0x000000ff;
        return red+","+green+","+blue;
        
	}

	/**
	 * Retrieves a random integer using the {@link Random}
	 * 
	 * @param min minimum range value
	 * @param max maximum range value
	 * 
	 * @return random integer within the range provided
	 */
	private static int getRandomNumberUsingNextInt(int min, int max) {
	    Random random = new Random();
	    return random.nextInt(max - min) + min;
	}
	
	/**
	 * Extracts background color from element screenshot by identifying the most prevalent color and returning that color
	 * @param element
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static ColorData extractBackgroundColor(URL screenshot_url, ColorData font_color) throws IOException {
		List<ColorUsageStat> color_data_list = new ArrayList<>();
		BufferedImage buffered_image = ImageUtils.readImageFromURL(screenshot_url);
		color_data_list.addAll( extractImageProperties(buffered_image)); //DO NOT CHANGE!!!  LOCAL BRUTE FORCE METHOD - NOTE: This method is used because GCP cloud vision appears to use PCA to reduce color space, causing some really wrong results. DO NOT CHANGE!!!
		
		double largest_pixel_percent = -1.0;
	    ColorUsageStat largest_color = null;
		//extract background colors
		
	    for(ColorUsageStat color_stat : color_data_list) {
			//get color most used for background color
			if(color_stat.getPixelPercent() >= largest_pixel_percent 
				&& !color_stat.getRGB().equals(font_color.rgb())
			) {
				largest_pixel_percent = color_stat.getPixelPercent();
				largest_color = color_stat;
			}
		}
	    
	    if(largest_color == null) {
	    	log.debug("Couldn't Identify largest color used, color data list size when extractBackgroundColor()  :  "+color_data_list.size());
	    	largest_color = new ColorUsageStat(255, 255, 255, 1, 1);
	    }
	    
		return new ColorData("rgb("+ largest_color.getRed()+","+largest_color.getGreen()+","+largest_color.getBlue()+")");
	}
	
	/**
	 * 
	 * @param buff_img
	 * @return
	 * @throws IOException
	 */
	public static String getChecksum(BufferedImage buff_img) throws IOException {
		assert buff_img != null;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; 
		
		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		baos.close();
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(data);
			byte[] thedigest = sha.digest(data);
			return Hex.encodeHexString(thedigest);
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating checksum of buffered image");
		}
		return "";

	}

	public static String createComposite(BufferedImage onload_screenshot, 
										 List<ElementState> element_states, 
										 PageState page_state, 
										 BrowserType browser) throws IOException 
	{
		URL page_url = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl(), false));

		BufferedImage composite_image = new BufferedImage(page_state.getFullPageWidth(), page_state.getFullPageHeight(), BufferedImage.TYPE_INT_ARGB);
		// get graphics to draw..
		Graphics2D graphics =composite_image.createGraphics();
		//draw the other image on it
		graphics.drawImage(onload_screenshot,0,0,null);
		
		for(ElementState element: element_states) {
			if(element.getScreenshotUrl().isEmpty()) {
				continue;
			}
			try {
				BufferedImage element_image = ImageIO.read(new URL(element.getScreenshotUrl()));
				graphics.drawImage(element_image, element.getXLocation(), element.getYLocation(), null);
			}
			catch(IOException e) {
				log.error("url is malformed :: "+element.getScreenshotUrl());
			}
		}
		
		String full_page_screenshot_checksum = ImageUtils.getChecksum(composite_image);
		String full_page_screenshot_url = GoogleCloudStorage.saveImage(composite_image, page_url.getHost(), full_page_screenshot_checksum, browser);
		
		return full_page_screenshot_url;
	}
	
	public static boolean areRowsMatching(BufferedImage current_screenshot, 
			int current_screenshot_row,
			BufferedImage original_image, 
			int original_screenshot_row
	) {
		for (int x = 0; x < current_screenshot.getWidth(); x++) {
			int current_screenshot_rgb = current_screenshot.getRGB(x, current_screenshot_row);
			int original_screenshot_rgb = original_image.getRGB(x, original_screenshot_row);
			if ( current_screenshot_rgb != original_screenshot_rgb) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean areWindowsMatching(BufferedImage current_screenshot, 
											int current_screenshot_row,
											BufferedImage original_image, 
											int original_screenshot_row,
											int window_height
	) {
		
		if( (original_screenshot_row + window_height-1) >= original_image.getHeight()
				|| (current_screenshot_row + window_height-1) >= current_screenshot.getHeight()
				|| current_screenshot_row < 0) {
			return false;
		}
		
		
		//perform random sampling to check equivalence
		
		Random random = new Random();
		int sample_size = (current_screenshot.getWidth() * window_height) / 2;
		for(int idx = 0; idx < sample_size; idx++) {
			int x = random.nextInt(current_screenshot.getWidth()-1);
			int y = random.nextInt(window_height-1);
			
			int current_screenshot_rgb = 0;
			try{
				current_screenshot_rgb = current_screenshot.getRGB(x, current_screenshot_row + y);
			}
			catch(Exception e) {
				log.warn("current row :: "+current_screenshot_row);
				log.warn("y value :: "+ y);
				log.warn("current row + y : "+(current_screenshot_row + y));
				log.warn("current screenshot height : "+ current_screenshot.getHeight());
				log.warn("x  :: "+x);
				log.warn("img width :: "+current_screenshot.getWidth());
			}
			int original_screenshot_rgb = original_image.getRGB(x, original_screenshot_row + y);
			if ( current_screenshot_rgb != original_screenshot_rgb) {
				return false;
			}
		}
		
		/*
		for (int x = 0; x < current_screenshot.getWidth(); x++) {
			for(int current_y = 0; current_y < window_height; current_y++) {
				int current_screenshot_rgb = current_screenshot.getRGB(x, current_screenshot_row+current_y);
				int original_screenshot_rgb = original_image.getRGB(x, original_screenshot_row+current_y);
				if ( current_screenshot_rgb != original_screenshot_rgb) {
					return false;
				}
			}
		}
		*/
		return true;
	}
	
	// convert BufferedImage to byte[]
    public static byte[] toByteArray(BufferedImage bi, String format)
        throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        byte[] bytes = baos.toByteArray();
        return bytes;

    }

	@Retry(name="gcp")
	public static BufferedImage readImageFromURL(URL full_page_screenshot_url) throws IOException {
		return ImageIO.read( full_page_screenshot_url );
	}
	
}
