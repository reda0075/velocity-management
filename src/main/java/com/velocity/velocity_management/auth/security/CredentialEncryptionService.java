package com.velocity.velocity_management.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Authenticated (AES-256-GCM) storage of the initial password handed to the administrator
 * when a collaborator was created, so that administrator can display it again later.
 *
 * <p>BCrypt is untouched and remains the only value ever compared during authentication.
 * This is a separate encrypted copy of the password issued at creation time; it never
 * authenticates anyone and is never exposed by a normal collaborator response.
 *
 * <p>The username is bound as additional authenticated data, so a stored ciphertext cannot
 * be moved to a different user row and decrypted there.
 */
@Component
public class CredentialEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;

    // Fixed per-application salt: the derived key must be identical on every boot.
    private static final byte[] KEY_DERIVATION_SALT =
            "velocity-management.credentials.v1".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(@Value("${app.credentials.encryption-key}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.credentials.encryption-key must be set (environment variable "
                            + "APP_CREDENTIAL_ENCRYPTION_KEY). Without it an administrator cannot "
                            + "retrieve the initial passwords issued at collaborator creation.");
        }
        this.encryptionKey = deriveKey(secret);
    }

    public String encrypt(String plaintext, String associatedUsername) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(associatedUsername.getBytes(StandardCharsets.UTF_8));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt the initial credentials", ex);
        }
    }

    public String decrypt(String payload, String associatedUsername) {
        try {
            byte[] raw = Base64.getDecoder().decode(payload);
            byte[] iv = Arrays.copyOfRange(raw, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(raw, IV_LENGTH_BYTES, raw.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(associatedUsername.getBytes(StandardCharsets.UTF_8));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Could not decrypt the initial credentials", ex);
        }
    }

    private static SecretKeySpec deriveKey(String secret) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    secret.toCharArray(),
                    KEY_DERIVATION_SALT,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH_BITS);
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(derived, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not derive the credentials encryption key", ex);
        }
    }
}