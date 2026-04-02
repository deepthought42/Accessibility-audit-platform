package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationConfigEncryptionTest {

    private IntegrationConfigEncryption encryption;

    @BeforeEach
    void setUp() {
        encryption = new IntegrationConfigEncryption();
    }

    private void setEncryptionKey(String key) throws Exception {
        Field field = IntegrationConfigEncryption.class.getDeclaredField("encryptionKey");
        field.setAccessible(true);
        field.set(encryption, key);
    }

    @Test
    void testEncryptNull() {
        assertNull(encryption.encrypt(null));
    }

    @Test
    void testDecryptNull() {
        assertNull(encryption.decrypt(null));
    }

    @Test
    void testEncryptPassthroughWhenNoKey() {
        String plainText = "hello world";
        assertEquals(plainText, encryption.encrypt(plainText));
    }

    @Test
    void testDecryptPassthroughWhenNoKey() {
        String cipherText = "hello world";
        assertEquals(cipherText, encryption.decrypt(cipherText));
    }

    @Test
    void testEncryptPassthroughWhenKeyEmpty() throws Exception {
        setEncryptionKey("");
        String plainText = "hello world";
        assertEquals(plainText, encryption.encrypt(plainText));
    }

    @Test
    void testDecryptPassthroughWhenKeyEmpty() throws Exception {
        setEncryptionKey("");
        String cipherText = "hello world";
        assertEquals(cipherText, encryption.decrypt(cipherText));
    }

    @Test
    void testEncryptPassthroughWhenKeyTooShort() throws Exception {
        setEncryptionKey("short");
        String plainText = "hello world";
        assertEquals(plainText, encryption.encrypt(plainText));
    }

    @Test
    void testDecryptPassthroughWhenKeyTooShort() throws Exception {
        setEncryptionKey("short");
        String cipherText = "hello world";
        assertEquals(cipherText, encryption.decrypt(cipherText));
    }

    @Test
    void testEncryptDecryptRoundTrip() throws Exception {
        setEncryptionKey("abcdefghijklmnop"); // 16 bytes
        String plainText = "sensitive data here";
        String encrypted = encryption.encrypt(plainText);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        String decrypted = encryption.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptDecryptRoundTripWithLongerKey() throws Exception {
        setEncryptionKey("abcdefghijklmnopqrstuvwxyz"); // 26 bytes, only first 16 used
        String plainText = "another secret message";
        String encrypted = encryption.encrypt(plainText);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, encryption.decrypt(encrypted));
    }

    @Test
    void testEncryptProducesDifferentOutputEachTime() throws Exception {
        setEncryptionKey("abcdefghijklmnop");
        String plainText = "same input";
        String encrypted1 = encryption.encrypt(plainText);
        String encrypted2 = encryption.encrypt(plainText);
        // Due to random IV, outputs should differ
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void testDecryptInvalidBase64ReturnsCipherText() throws Exception {
        setEncryptionKey("abcdefghijklmnop");
        // Not valid base64 or too short after decode
        String invalidCipher = "not-valid-base64!!!";
        String result = encryption.decrypt(invalidCipher);
        assertEquals(invalidCipher, result);
    }

    @Test
    void testDecryptTooShortCipherTextReturnsCipherText() throws Exception {
        setEncryptionKey("abcdefghijklmnop");
        // Valid base64 but too short (less than 12 bytes after decode)
        String shortCipher = java.util.Base64.getEncoder().encodeToString(new byte[5]);
        String result = encryption.decrypt(shortCipher);
        assertEquals(shortCipher, result);
    }

    @Test
    void testEncryptDecryptEmptyString() throws Exception {
        setEncryptionKey("abcdefghijklmnop");
        String plainText = "";
        String encrypted = encryption.encrypt(plainText);
        assertNotNull(encrypted);
        assertEquals(plainText, encryption.decrypt(encrypted));
    }

    @Test
    void testEncryptDecryptSpecialCharacters() throws Exception {
        setEncryptionKey("abcdefghijklmnop");
        String plainText = "{\"token\": \"abc123!@#$%\", \"url\": \"https://example.com\"}";
        String encrypted = encryption.encrypt(plainText);
        assertEquals(plainText, encryption.decrypt(encrypted));
    }
}
