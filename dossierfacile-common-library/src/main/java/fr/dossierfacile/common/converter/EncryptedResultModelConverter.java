package fr.dossierfacile.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.exceptions.ParsingException;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.common.utils.MapperUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Converter(autoApply = true)
public class EncryptedResultModelConverter implements AttributeConverter<ResultModel, String> {

    private static final ObjectMapper OBJECT_MAPPER = MapperUtil.newObjectMapper();
    private static final String ENV_KEY_NAME = "DOSSIERFACILE_DOCUMENT_IA_RESULT_ENCRYPTION_KEY";
    private static final String PROP_KEY_NAME = "dossierfacile.document.ia.result.encryption.key";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String VERSION_PREFIX = "v1";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${dossierfacile.document.ia.result.encryption.key}")
    private String encryptionKey;

    private volatile SecretKey cachedKey;

    @Override
    public String convertToDatabaseColumn(ResultModel attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(attribute);
            return encrypt(json);
        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed to serialize ResultModel", e);
        }
    }

    @Override
    public ResultModel convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            String json = decrypt(dbData);
            return OBJECT_MAPPER.readValue(json, ResultModel.class);
        } catch (Exception e) {
            throw new ParsingException("Failed to deserialize ResultModel", e);
        }
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return VERSION_PREFIX + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new ParsingException("Failed to encrypt ResultModel", e);
        }
    }

    private String decrypt(String encrypted) {
        String[] parts = encrypted.split(":", 3);
        if (parts.length != 3 || !VERSION_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported encrypted payload format");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new ParsingException("Failed to decrypt ResultModel", e);
        }
    }

    private SecretKey getSecretKey() {
        SecretKey localKey = cachedKey;
        if (localKey != null) {
            return localKey;
        }
        synchronized (this) {
            if (cachedKey == null) {
                cachedKey = loadSecretKey();
            }
            return cachedKey;
        }
    }

    private SecretKey loadSecretKey() {
        // Fallback for simple unit test !
        String keyValue = (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : null;
        if (keyValue == null || keyValue.isBlank()) {
            keyValue = System.getProperty(PROP_KEY_NAME);
        }
        if (keyValue == null || keyValue.isBlank()) {
            keyValue = System.getenv(ENV_KEY_NAME);
        }
        if (keyValue == null || keyValue.isBlank()) {
            throw new IllegalStateException("Missing encryption key !");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Encryption key must be base64", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("Invalid AES key length: " + keyBytes.length + " bytes");
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}
