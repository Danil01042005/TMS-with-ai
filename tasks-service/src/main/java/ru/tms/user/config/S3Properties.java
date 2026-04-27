package ru.tms.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    /**
     * S3 endpoint для S3-совместимых провайдеров (MinIO, локальные шлюзы и т.п.).
     * Если пусто - используется стандартная конфигурация AWS SDK.
     * В Docker обычно {@code http://minio:9000} — доступен только из контейнеров.
     */
    private String endpoint;

    /**
     * Публичный endpoint для pre-signed URL (то, что открывает браузер пользователя).
     * Например {@code http://localhost:9000}. Если пусто — для presigner используется {@link #endpoint}.
     */
    private String publicEndpoint;

    private String region = "us-east-1";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /**
     * TTL для pre-signed URL (сек).
     */
    private long presignTtlSeconds = 300L;

    /**
     * Для S3-совместимых хранилищ (часто MinIO) может потребоваться path-style.
     */
    private boolean pathStyleAccessEnabled = false;
}

