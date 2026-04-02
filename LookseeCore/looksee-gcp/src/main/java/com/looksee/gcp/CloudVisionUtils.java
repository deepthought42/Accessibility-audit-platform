package com.looksee.gcp;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.LocationInfo;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebLabel;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.protobuf.ByteString;
import com.looksee.models.ColorUsageStat;
import com.looksee.models.ImageFaceAnnotation;
import com.looksee.models.ImageLandmarkInfo;
import com.looksee.models.ImageSearchAnnotation;
import com.looksee.models.Label;
import com.looksee.models.LatLng;
import com.looksee.models.Logo;
import com.looksee.utils.ImageUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for analyzing images using the Google Cloud Vision API.
 */
@NoArgsConstructor
public class CloudVisionUtils {
	private static Logger log = LoggerFactory.getLogger(CloudVisionUtils.class);
	
    /**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return List of {@link String}s
	 * @throws IOException
	 *
	 * precondition: buffered_image != null
	 */
	public static List<String> extractImageText(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<String> text_values = new ArrayList<>(); 
		List<AnnotateImageRequest> requests = new ArrayList<>();
		
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();
			
			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					log.error("Error: %s%n", res.getError().getMessage());
					return new ArrayList<>();
				}
				
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
					text_values.add(annotation.getDescription());
				}
			}
		}
        return text_values;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return Set of {@link Label}s
	 * @throws IOException
	 *
	 * precondition: buffered_image != null
	 */
	public static Set<Label> extractImageLabels(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<AnnotateImageRequest> requests = new ArrayList<>();
		Set<Label> labels = new HashSet<>();
		
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					log.error("Error: %s%n", res.getError().getMessage());
					return new HashSet<>();
				}
		
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
					labels.add(new Label(annotation.getDescription(),
											annotation.getScore()));
				}
			}
		}
		
		return labels;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return Set of {@link ImageLandmarkInfo}s
	 * @throws IOException
	 *
	 * precondition: buffered_image != null
	 */
	public static Set<ImageLandmarkInfo> extractImageLandmarks(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;
		
		List<AnnotateImageRequest> requests = new ArrayList<>();
		Set<ImageLandmarkInfo> landmark_info_set = new HashSet<>();
		
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
			
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					log.error("Error: %s%n", res.getError().getMessage());
					return new HashSet<>();
				}
		
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
					Set<LatLng> locations = new HashSet<LatLng>();
					for(LocationInfo info: annotation.getLocationsList()) {
						locations.add(new LatLng(info.getLatLng().getLatitude(), info.getLatLng().getLongitude()));
					}
					
					landmark_info_set.add(new ImageLandmarkInfo(locations, annotation.getDescription(), annotation.getScore()));      		
				}
			}
		}
		
		return landmark_info_set;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return Set of {@link ImageFaceAnnotation}s
	 * @throws IOException
	 *
	 * precondition: buffered_image != null
	 */
	public static Set<ImageFaceAnnotation> extractImageFaces(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<AnnotateImageRequest> requests = new ArrayList<>();
		Set<ImageFaceAnnotation> face_annotations = new HashSet<>();
		
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);
	
		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					log.error("Error: %s%n", res.getError().getMessage());
					return new HashSet<>();
				}
		
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
					face_annotations.add(new ImageFaceAnnotation(annotation.getAngerLikelihood(),
																	annotation.getJoyLikelihood(),
																	annotation.getBlurredLikelihood(),
																	annotation.getHeadwearLikelihood(),
																	annotation.getSorrowLikelihood(),
																	annotation.getSurpriseLikelihood(),
																	annotation.getUnderExposedLikelihood(),
																	annotation.getBoundingPoly()));
				}
			}
		}
		
		return face_annotations;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return Set of {@link Logo}s
	 * @throws IOException
	 * 
	 * precondition: buffered_image != null
	 */
	public static Set<Logo> extractImageLogos(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<AnnotateImageRequest> requests = new ArrayList<>();
		Set<Logo> logos = new HashSet<>();
		
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.LOGO_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					return new HashSet<>();
				}
		
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
					logos.add(new Logo(annotation.getDescription(),
										annotation.getLocale(),
										annotation.getScore(),
										annotation.getBoundingPoly()));
				}
			}
		}
		
		return logos;
	}
	
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return {@link ImageSearchAnnotation}
	 * @throws IOException
	 * 
	 * precondition: buffered_image != null
	 */
	public static ImageSearchAnnotation searchWebForImageUsage(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<AnnotateImageRequest> requests = new ArrayList<>();
		ImageSearchAnnotation image_search_annotation = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					log.error("Error: %s%n", res.getError().getMessage());
					return null;
				}
		
				// Search the web for usages of the image. You could use these signals later
				// for user input moderation or linking external references.
				// For a full list of available annotations, see http://g.co/cloud/vision/docs
				WebDetection annotation = res.getWebDetection();
				
				Set<String> best_guess_labels = new HashSet<>();
				for (WebLabel label : annotation.getBestGuessLabelsList()) {
					best_guess_labels.add(label.getLabel());
				}
				
				Set<String> similar_images = new HashSet<>();
				for (WebPage page : annotation.getPagesWithMatchingImagesList()) {
					similar_images.add(page.getUrl());
				}
				
				for (WebImage image : annotation.getPartialMatchingImagesList()) {
					similar_images.add(image.getUrl());
				}
				
				Set<String> fully_matching_images = new HashSet<>();
				for (WebImage image : annotation.getFullMatchingImagesList()) {
					fully_matching_images.add(image.getUrl());
				}
				
				for (WebImage image : annotation.getVisuallySimilarImagesList()) {
					similar_images.add(image.getUrl());
				}

				image_search_annotation = new ImageSearchAnnotation(  best_guess_labels,
																		fully_matching_images,
																		similar_images);
			}
		}
		
		return image_search_annotation;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 *
	 * @param buffered_image the {@link BufferedImage}
	 *
	 * @return List of {@link ColorUsageStat}s
	 * @throws IOException
	 *
	 * precondition: buffered_image != null
	 */
	public static List<ColorUsageStat> extractImageProperties(BufferedImage buffered_image) throws IOException {
		assert buffered_image != null;

		List<ColorUsageStat> color_usage_stats = new ArrayList<>();
		
		List<AnnotateImageRequest> requests = new ArrayList<>();
		buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_image, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					System.out.format("Error: %s%n", res.getError().getMessage());
					return color_usage_stats;
				}
		
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
				// For full list of available annotations, see http://g.co/cloud/vision/docs
				for (ColorInfo color : colors.getColorsList()) {
					/*
					System.out.format(
						"fraction: %f%nr: %f, g: %f, b: %f, score: %f%n",
						color.getPixelFraction(),
						color.getColor().getRed(),
						color.getColor().getGreen(),
						color.getColor().getBlue(),
						color.getScore());
						*/
					ColorUsageStat color_stat = new ColorUsageStat(color.getColor().getRed(), color.getColor().getGreen(), color.getColor().getBlue(), color.getPixelFraction(), color.getScore());
					color_usage_stats.add(color_stat);
				}
			}
		}
		
		return color_usage_stats;
	}
	
	/**
	 * Detects safe search properties from the specified local image.
	 *
	 * @param buffered_img the {@link BufferedImage}
	 *
	 * @return {@link ImageSafeSearchAnnotation}
	 * @throws IOException
	 *
	 * precondition: buffered_img != null
	 */
	public static ImageSafeSearchAnnotation detectSafeSearch(BufferedImage buffered_img) throws IOException {
		assert buffered_img != null;

		List<AnnotateImageRequest> requests = new ArrayList<>();
		ImageSafeSearchAnnotation safe_search_annotation = null;
	
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(buffered_img, "png", os);
		InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		
		ByteString imgBytes = ByteString.readFrom(input_stream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build();
		AnnotateImageRequest request =
			AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					System.out.format("Error: %s%n", res.getError().getMessage());
					return null;
				}

				// For full list of available annotations, see http://g.co/cloud/vision/docs
				SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
				safe_search_annotation = new ImageSafeSearchAnnotation(annotation.getSpoof().name(),
																		annotation.getMedical().name(),
																		annotation.getAdult().name(),
																		annotation.getViolence().name(),
																		annotation.getRacy().name());
			}
		}
		
		return safe_search_annotation;
	}
}