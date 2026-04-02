package com.looksee.pageBuilder.gcp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.looksee.pageBuilder.models.enums.BrowserType;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Handles uploading files to Google Cloud Storage
 */
@Retry(name = "gcp")
public class GoogleCloudStorage {
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(GoogleCloudStorage.class);

	private static String bucket_name     = "look-see-data";
	
	public static String saveImage(BufferedImage image, 
								   String domain, 
								   String checksum, 
								   BrowserType browser
   ) throws IOException {
		assert image != null;
		assert domain != null;
		assert !domain.isEmpty();
		assert checksum != null;
		assert !checksum.isEmpty();
		assert browser != null;
		
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Bucket bucket = storage.get(bucket_name);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( image, "png", baos );
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		String stripped_domain = domain.replace(".", "").replace("/", "").replace(":", "").replace("https", "").replace("http", "");
		String key = stripped_domain+checksum+browser;
		String file_name = key+".png";
		Blob blob = bucket.get(file_name);
		if(blob != null && blob.exists()) {
        	return blob.getMediaLink();
        }
		
		//blob = bucket.create(key+".png", imageInByte);
		BlobId blobId = BlobId.of(bucket_name, file_name);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();
		try (WriteChannel writer = storage.writer(blobInfo)) {
			writer.write(ByteBuffer.wrap(imageInByte, 0, imageInByte.length));
		} catch (IOException ex) {
		   throw ex;
		}
		
		blob = bucket.get(file_name);
		if(blob != null && blob.exists()) {
        	return blob.getMediaLink();
        }
		else {
			throw new IOException("Couldn't find blob after upload");
		}
    }
	
	public static BufferedImage getImage(String domain, 
										 String element_key, 
										 BrowserType browser
	) throws IOException {
		assert domain != null;
		assert !domain.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();
		assert browser != null;
		
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Bucket bucket = storage.get(bucket_name);


		String host_key = org.apache.commons.codec.digest.DigestUtils.sha256Hex(domain);
		Blob blob = bucket.get(host_key+""+element_key+browser+".png");
		InputStream inputStream = Channels.newInputStream(blob.reader());

        return ImageIO.read(inputStream);
    }
	
	public static BufferedImage getImage(String image_url) throws IOException {
		assert image_url != null;
		assert !image_url.isEmpty();
		
//		Storage storage = StorageOptions.getDefaultInstance().getService();
//		Bucket bucket = storage.get(bucketName);

//		Blob blob = bucket.get(image_url);
		return ImageIO.read(new URL(image_url));
    }
}