package com.looksee.gcp;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.looksee.browsing.enums.BrowserType;
import io.github.resilience4j.retry.annotation.Retry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

/**
 * Handles uploading files to Google Cloud Storage
 */
@Retry(name = "gcp")
@Service
@ConditionalOnClass(name = "com.google.cloud.storage.Storage")
@ConditionalOnBean(Storage.class)
public class GoogleCloudStorage {
	
	private final Storage storage;
    private final String bucketName;
    private final String publicUrl;

	/**
	 * Constructor for {@link GoogleCloudStorage}
	 *
	 * @param storage the storage to use
	 * @param gcsProperties the properties to use
	 *
	 * precondition: storage != null
	 * precondition: gcsProperties != null
	 */
	public GoogleCloudStorage(Storage storage, GoogleCloudStorageProperties gcsProperties) {
		assert storage != null;
		assert gcsProperties != null;

        this.storage = storage;
        this.bucketName = gcsProperties.getBucketName();
        this.publicUrl = gcsProperties.getPublicUrl();
    }

	/**
	 * Uploads HTML content to Google Cloud Storage
	 * @param content the content to upload
	 * @param key the key to use
	 * @return the URL of the uploaded content
	 * @throws IOException if an error occurs
	 *
	 * precondition: content != null
	 * precondition: !content.isEmpty()
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public String uploadHtmlContent(String content, String key) throws IOException {
		assert content != null;
		assert !content.isEmpty();
		assert key != null;
		assert !key.isEmpty();

        BlobId blobId = BlobId.of(bucketName, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType("text/html")
            .setCacheControl("public, max-age=31536000")
            .build();
        
        try (WriteChannel writer = storage.writer(blobInfo)) {
            writer.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        }
        return publicUrl + "/" + key;
    }

	/**
	 * Retrieves HTML content from Google Cloud Storage
	 * @param gcsUrl the URL of the content to retrieve
	 * @return the content
	 *
	 * precondition: gcsUrl != null
	 * precondition: !gcsUrl.isEmpty()
	 */
    public String getHtmlContent(String gcsUrl) {
		assert gcsUrl != null;
		assert !gcsUrl.isEmpty();

        String key = gcsUrl.replace(publicUrl + "/", "");
        Blob blob = storage.get(bucketName, key);
        return new String(blob.getContent(), StandardCharsets.UTF_8);
    }
	
	/**
	 * Saves an image to Google Cloud Storage
	 * @param image the image to save
	 * @param domain the domain of the image
	 * @param checksum the checksum of the image
	 * @param browser the browser of the image
	 * @return the URL of the saved image
	 * @throws IOException if an error occurs
	 *
	 * precondition: image != null
	 * precondition: domain != null
	 * precondition: !domain.isEmpty()
	 * precondition: checksum != null
	 * precondition: !checksum.isEmpty()
	 */
	public String saveImage(BufferedImage image,
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
		Bucket bucket = storage.get(bucketName);
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
		BlobId blobId = BlobId.of(bucketName, file_name);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();
		try (WriteChannel writer = storage.writer(blobInfo)) {
			writer.write(ByteBuffer.wrap(imageInByte, 0, imageInByte.length));
		}
		
		blob = bucket.get(file_name);
		if(blob != null && blob.exists()) {
			return blob.getMediaLink();
        }
		else {
			throw new IOException("Couldn't find blob after upload");
		}
    }
	
	/**
	 * Retrieves an image from Google Cloud Storage
	 * @param domain the domain of the image
	 * @param element_key the key of the image
	 * @param browser the browser of the image
	 * @return the image
	 * @throws IOException if an error occurs
	 *
	 * precondition: domain != null
	 * precondition: !domain.isEmpty()
	 * precondition: element_key != null
	 * precondition: !element_key.isEmpty()
	 */
	public BufferedImage getImage(String domain,
										 String element_key,
										 BrowserType browser
	) throws IOException {
		assert domain != null;
		assert !domain.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();
		assert browser != null;
		
		Storage storage = StorageOptions.getDefaultInstance().getService();
		Bucket bucket = storage.get(bucketName);


		String host_key = org.apache.commons.codec.digest.DigestUtils.sha256Hex(domain);
		Blob blob = bucket.get(host_key+""+element_key+browser+".png");
		InputStream inputStream = Channels.newInputStream(blob.reader());

        return ImageIO.read(inputStream);
    }
	
	/**
	 * Retrieves an image from a URL
	 * @param image_url the URL of the image to retrieve
	 * @return the image
	 * @throws IOException if an error occurs
	 *
	 * precondition: image_url != null
	 * precondition: !image_url.isEmpty()
	 */
	public BufferedImage getImage(String image_url) throws IOException {
		assert image_url != null;
		assert !image_url.isEmpty();
		
		return ImageIO.read(new URL(image_url));
    }
}