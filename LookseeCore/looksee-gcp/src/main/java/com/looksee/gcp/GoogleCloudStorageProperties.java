package com.looksee.gcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for Google Cloud Storage.
 */
@ConfigurationProperties(prefix = "gcs.bucket")
@ConstructorBinding
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GoogleCloudStorageProperties {

    private String bucketName;
    private String publicUrl;
}
