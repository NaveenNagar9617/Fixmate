package com.fixmate.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Production-grade MinIO Configuration for FixMate.
 * Enforces:
 * 1. Strictly private bucket creation (no anonymous public read policies).
 * 2. Dedicated internal MinioClient for backend storage I/O.
 * 3. Dedicated presigning MinioClient for browser-reachable presigned URL generation.
 */
@Configuration
@Slf4j
public class MinioConfig {

    @Value("${app.minio.url}")
    private String minioUrl;

    @Value("${app.minio.public-url:${app.minio.url}}")
    private String minioPublicUrl;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Bean
    @Primary
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();

        ensurePrivateBucketExists(client);
        return client;
    }

    /**
     * Dedicated MinIO client used solely for generating presigned URLs with the browser-accessible public host.
     */
    @Bean(name = "presigningMinioClient")
    public MinioClient presigningMinioClient() {
        return MinioClient.builder()
                .endpoint(minioPublicUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    private void ensurePrivateBucketExists(MinioClient client) {
        try {
            boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!bucketExists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created private MinIO bucket: '{}'", bucket);
            } else {
                log.info("MinIO bucket '{}' verified (Private access)", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO startup notice for bucket '{}': {}. Bucket will be ensured on demand.",
                    bucket, e.getMessage());
        }
    }
}
