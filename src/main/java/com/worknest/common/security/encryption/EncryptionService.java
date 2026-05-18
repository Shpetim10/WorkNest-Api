package com.worknest.common.security.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EncryptionService {

    private static final String ENCRYPTION_PREFIX = "ENCv1:";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String keyBase64;

    public EncryptionService(@Value("${app.encryption.key-base64:}") String keyBase64) {
        this.keyBase64 = keyBase64;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (plaintext.startsWith(ENCRYPTION_PREFIX)) {
            return plaintext;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireEncryptionKey(), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt field value", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }
        if (!encryptedValue.startsWith(ENCRYPTION_PREFIX)) {
            return encryptedValue;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(ENCRYPTION_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted payload is invalid");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireEncryptionKey(), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt field value", exception);
        }
    }

    public String hmacSha256Hex(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(requireHmacKey(), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash field value", exception);
        }
    }

    public String normalizeNipt(String nipt) {
        if (!StringUtils.hasText(nipt)) {
            return null;
        }
        return nipt.trim().toUpperCase(Locale.ROOT);
    }

    private byte[] requireEncryptionKey() {
        if (!StringUtils.hasText(keyBase64)) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY_BASE64 must be configured before encrypted fields can be used");
        }

        byte[] decoded = Base64.getDecoder().decode(keyBase64);
        if (decoded.length != 32) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY_BASE64 must decode to exactly 32 bytes for AES-256-GCM");
        }
        return decoded;
    }

    private byte[] requireHmacKey() {
        byte[] encryptionKey = requireEncryptionKey();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("worknest-blind-index".getBytes(StandardCharsets.UTF_8));
            digest.update(encryptionKey);
            return digest.digest();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to derive HMAC key", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
