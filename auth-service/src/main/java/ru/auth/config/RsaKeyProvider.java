package ru.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class RsaKeyProvider {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyProvider.class);
    private static final String KEY_ID_FILE = "rsa-key-id.txt";
    private static final String PUBLIC_KEY_FILE = "rsa-public-key.pem";
    private static final String PRIVATE_KEY_FILE = "rsa-private-key.pem";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;
    private final String keysDirectory;

    public RsaKeyProvider(@Value("${auth.jwt.keys.directory:./keys}") String keysDirectory) {
        this.keysDirectory = keysDirectory;
        try {
            Path keysPath = Paths.get(keysDirectory);
            
            // Создаем директорию если не существует
            if (!Files.exists(keysPath)) {
                Files.createDirectories(keysPath);
                logger.info("Created keys directory: {}", keysPath.toAbsolutePath());
            }

            // Устанавливаем безопасные права доступа (только для владельца)
            try {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(keysPath, perms);
            } catch (UnsupportedOperationException e) {
                // Windows не поддерживает PosixFilePermission, игнорируем
                logger.debug("PosixFilePermission not supported on this platform");
            }

            // Пытаемся загрузить существующие ключи
            KeyPair loadedKeys = loadKeysFromFile(keysPath);
            
            if (loadedKeys != null) {
                logger.info("Loaded existing RSA keys from: {}", keysPath.toAbsolutePath());
                this.publicKey = (RSAPublicKey) loadedKeys.getPublic();
                this.privateKey = (RSAPrivateKey) loadedKeys.getPrivate();
                this.keyId = loadKeyId(keysPath);
            } else {
                // Генерируем новые ключи
                logger.info("Generating new RSA keys...");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.publicKey = (RSAPublicKey) kp.getPublic();
                this.privateKey = (RSAPrivateKey) kp.getPrivate();
                this.keyId = UUID.randomUUID().toString();
                
                // Сохраняем ключи в файлы
                saveKeysToFile(keysPath, kp, this.keyId);
                logger.info("Saved new RSA keys to: {}", keysPath.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize RSA keys", e);
            throw new IllegalStateException("Cannot initialize RSA keys", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getJwksJson() {
        String n = base64Url(publicKey.getModulus().toByteArray());
        String e = base64Url(publicKey.getPublicExponent().toByteArray());
        String jwk = "{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + keyId + "\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}";
        return "{\"keys\":[" + jwk + "]}";
    }

    private static String base64Url(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private KeyPair loadKeysFromFile(Path keysPath) {
        try {
            Path publicKeyPath = keysPath.resolve(PUBLIC_KEY_FILE);
            Path privateKeyPath = keysPath.resolve(PRIVATE_KEY_FILE);

            if (!Files.exists(publicKeyPath) || !Files.exists(privateKeyPath)) {
                return null;
            }

            // Читаем публичный ключ
            String publicKeyPem = Files.readString(publicKeyPath);
            byte[] publicKeyBytes = decodePem(publicKeyPem, "PUBLIC KEY");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);

            // Читаем приватный ключ
            String privateKeyPem = Files.readString(privateKeyPath);
            byte[] privateKeyBytes = decodePem(privateKeyPem, "PRIVATE KEY");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            logger.warn("Failed to load keys from file, will generate new ones", e);
            return null;
        }
    }

    private String loadKeyId(Path keysPath) {
        try {
            Path keyIdPath = keysPath.resolve(KEY_ID_FILE);
            if (Files.exists(keyIdPath)) {
                return Files.readString(keyIdPath).trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to load key ID from file", e);
        }
        return UUID.randomUUID().toString();
    }

    private void saveKeysToFile(Path keysPath, KeyPair keyPair, String keyId) {
        try {
            // Сохраняем публичный ключ
            Path publicKeyPath = keysPath.resolve(PUBLIC_KEY_FILE);
            String publicKeyPem = encodePem(keyPair.getPublic().getEncoded(), "PUBLIC KEY");
            Files.writeString(publicKeyPath, publicKeyPem);
            setSecurePermissions(publicKeyPath);

            // Сохраняем приватный ключ
            Path privateKeyPath = keysPath.resolve(PRIVATE_KEY_FILE);
            String privateKeyPem = encodePem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY");
            Files.writeString(privateKeyPath, privateKeyPem);
            setSecurePermissions(privateKeyPath);

            // Сохраняем key ID
            Path keyIdPath = keysPath.resolve(KEY_ID_FILE);
            Files.writeString(keyIdPath, keyId);
            setSecurePermissions(keyIdPath);

            logger.info("RSA keys saved successfully to: {}", keysPath.toAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save keys to file", e);
            throw new IllegalStateException("Cannot save RSA keys", e);
        }
    }

    private void setSecurePermissions(Path filePath) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(filePath, perms);
        } catch (UnsupportedOperationException e) {
            // Windows не поддерживает PosixFilePermission, игнорируем
            logger.debug("PosixFilePermission not supported on this platform");
        } catch (Exception e) {
            logger.warn("Failed to set secure permissions on key file", e);
        }
    }

    private String encodePem(byte[] keyBytes, String keyType) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyBytes);
        return String.format("-----BEGIN %s-----\n%s\n-----END %s-----\n", keyType, base64, keyType);
    }

    private byte[] decodePem(String pem, String keyType) {
        String header = String.format("-----BEGIN %s-----", keyType);
        String footer = String.format("-----END %s-----", keyType);
        
        String base64 = pem
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s", "");
        
        return Base64.getDecoder().decode(base64);
    }
}





