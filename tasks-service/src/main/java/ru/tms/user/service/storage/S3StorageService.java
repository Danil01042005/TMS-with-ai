package ru.tms.user.service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tms.user.config.S3Properties;
import ru.tms.user.config.UploadProperties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import ru.tms.user.service.exception.FileStorageUnavailableException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Properties s3Properties;
    private final UploadProperties uploadProperties;

    @jakarta.annotation.PostConstruct
    private void validateConfig() {
        if (s3Properties.getBucket() == null || s3Properties.getBucket().isBlank()) {
            throw new IllegalArgumentException("S3 bucket is not configured (app.s3.bucket)");
        }
    }

    public String upload(MultipartFile file, String ownerId) {
        validateUpload(file);

        String originalName = file.getOriginalFilename();
        String safeOriginalName = sanitizeFileName(originalName);
        String extension = extractExtension(safeOriginalName);

        // Ключ: ownerId/yyyy/MM/<uuid>.<ext>
        String objectKey = "%s/%s/%s/%s.%s"
                .formatted(
                        ownerId,
                        Instant.now().atZone(java.time.ZoneOffset.UTC).getYear(),
                        String.format("%02d", Instant.now().atZone(java.time.ZoneOffset.UTC).getMonthValue()),
                        UUID.randomUUID(),
                        extension.isBlank() ? "bin" : extension
                );

        String contentType = resolveContentType(file, extension);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey);

        // В некоторых реализациях S3-совместимого API установка contentType может быть критичной.
        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        PutObjectRequest request = requestBuilder.build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read upload stream", e);
        } catch (S3Exception e) {
            String details = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new FileStorageUnavailableException("File storage is unavailable: " + details, e);
        } catch (Exception e) {
            throw new FileStorageUnavailableException("File storage is unavailable", e);
        }

        return objectKey;
    }

    /**
     * Генерирует pre-signed URL на скачивание/открытие объекта.
     */
    public String presignedGetUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();

        try {
            PresignedGetObjectRequest presigned = presigner.presignGetObject(req ->
                    req.getObjectRequest(getObjectRequest)
                            .signatureDuration(java.time.Duration.ofSeconds(s3Properties.getPresignTtlSeconds()))
            );
            return presigned.url().toString();
        } catch (Exception e) {
            throw new FileStorageUnavailableException("Failed to create file download URL", e);
        }
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is blank");
        }
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            // S3/MinIO: удаление несуществующего ключа не должно блокировать очистку метаданных в БД.
            if ("NoSuchKey".equals(code) || "NotFound".equalsIgnoreCase(code) || e.statusCode() == 404) {
                return;
            }
            String details = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw new FileStorageUnavailableException("Failed to delete file from storage: " + details, e);
        } catch (Exception e) {
            throw new FileStorageUnavailableException("Failed to delete file from storage", e);
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (file.getSize() > uploadProperties.getMaxBytes()) {
            throw new IllegalArgumentException("File is too large. Max bytes=" + uploadProperties.getMaxBytes());
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);

        var allowedContentTypes = parseList(uploadProperties.getAllowedContentTypes());
        var allowedExtensions = parseList(uploadProperties.getAllowedExtensions());

        boolean contentTypeAllowed = contentType != null && allowedContentTypes.contains(contentType);
        boolean extensionAllowed = !extension.isBlank() && allowedExtensions.contains(extension.toLowerCase());

        // Если contentType не распознан - ориентируемся на расширение.
        if (!(contentTypeAllowed || extensionAllowed)) {
            throw new IllegalArgumentException("File type is not allowed");
        }
    }

    private String resolveContentType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) return contentType;
        // В fallback используем extension; если неизвестен - оставляем null.
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> null;
        };
    }

    private Set<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        String[] parts = csv.split(",");
        return java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "file";
        String trimmed = name.trim();
        if (trimmed.isBlank()) return "file";
        // Убираем потенциально опасные символы.
        return trimmed.replaceAll("[^a-zA-Z0-9\\._\\-\\(\\) ]+", "_");
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return "";
        return fileName.substring(idx + 1).toLowerCase();
    }
}

