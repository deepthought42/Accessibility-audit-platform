package com.looksee.llm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.Value;

/**
 * A single image attached to a multimodal LLM request.
 *
 * <p>Use {@link #sha256()} when computing cache keys so that identical image
 * payloads hit the same cache entry regardless of how they were sourced.
 */
@Value
public class ImagePart {

    byte[] bytes;
    String mimeType;

    public static ImagePart of(byte[] bytes, String mimeType) {
        return new ImagePart(bytes, mimeType);
    }

    /**
     * Stable hex-encoded SHA-256 of the image bytes.
     */
    public String sha256() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by every JRE; this is unreachable.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
