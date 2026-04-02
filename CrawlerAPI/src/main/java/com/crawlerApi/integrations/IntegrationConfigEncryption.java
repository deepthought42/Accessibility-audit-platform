package com.crawlerApi.integrations;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Optional encryption at rest for integration config. When integrations.encryption.key is not set, pass-through.
 * When set (16+ bytes for AES-128-GCM), config is encrypted before save and decrypted after load.
 */
@Component
public class IntegrationConfigEncryption {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConfigEncryption.class);
    private static final String ALG = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${integrations.encryption.key:}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            return plainText;
        }
        try {
            byte[] key = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (key.length < 16) {
                log.warn("integrations.encryption.key too short, skipping encryption");
                return plainText;
            }
            byte[] key16 = new byte[16];
            System.arraycopy(key, 0, key16, 0, Math.min(key.length, 16));
            SecretKeySpec spec = new SecretKeySpec(key16, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.ENCRYPT_MODE, spec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.warn("Integration config encryption failed, storing plain", e);
            return plainText;
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length <= GCM_IV_LENGTH) {
                return cipherText;
            }
            byte[] key = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (key.length < 16) {
                return cipherText;
            }
            byte[] key16 = new byte[16];
            System.arraycopy(key, 0, key16, 0, 16);
            SecretKeySpec spec = new SecretKeySpec(key16, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.DECRYPT_MODE, spec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Integration config decryption failed, returning as-is (may be plain)", e);
            return cipherText;
        }
    }
}
