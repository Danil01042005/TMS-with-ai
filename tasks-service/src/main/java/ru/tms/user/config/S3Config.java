package ru.tms.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties({S3Properties.class, UploadProperties.class})
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        var region = Region.of(props.getRegion());

        // Если ключи явно заданы - используем их, иначе полагаемся на провайдер окружения AWS SDK.
        var credentialsProvider = (props.getAccessKey() != null && !props.getAccessKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()))
                : DefaultCredentialsProvider.create();

        var builder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder().build());

        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        // Для S3-совместимых (MinIO и т.п.) часто требуется path-style.
        builder.forcePathStyle(props.isPathStyleAccessEnabled());

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        var region = Region.of(props.getRegion());

        var credentialsProvider = (props.getAccessKey() != null && !props.getAccessKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()))
                : DefaultCredentialsProvider.create();

        var builder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.isPathStyleAccessEnabled())
                        .build());

        String presignEndpoint = props.getPublicEndpoint();
        if (presignEndpoint == null || presignEndpoint.isBlank()) {
            presignEndpoint = props.getEndpoint();
        }
        if (presignEndpoint != null && !presignEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(presignEndpoint));
        }

        return builder.build();
    }
}

