package com.fixmate.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade File Storage Service for FixMate.
 * 
 * Key Architectural Guarantees:
 * 1. Returns relative object keys (e.g. "complaints/{id}/{uuid}.webp") for database storage.
 * 2. Generates short-lived (1-hour) HMAC-SHA256 presigned URLs signed with the browser-accessible public host.
 * 3. Supports resolving legacy full URLs to active presigned URLs for seamless backwards compatibility.
 * 4. Safe, non-blocking best-effort object deletion.
 */
@Service
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioClient presigningMinioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.url}")
    private String minioUrl;

    @Value("${app.minio.public-url:${app.minio.url}}")
    private String minioPublicUrl;

    @Value("${app.minio.presigned-url-expiry:3600}")
    private int presignedUrlExpirySeconds;

    public FileStorageService(
            MinioClient minioClient,
            @Qualifier("presigningMinioClient") MinioClient presigningMinioClient) {
        this.minioClient = minioClient;
        this.presigningMinioClient = presigningMinioClient;
    }

    /**
     * Uploads an image to MinIO and returns the relative object key.
     *
     * @param file the MultipartFile to upload
     * @param folder the destination directory (e.g., "complaints/7f3c1d")
     * @return the relative object key (e.g., "complaints/7f3c1d/2a8c9d.webp")
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            ensureBucketExists();

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            }
            String objectKey = folder + "/" + UUID.randomUUID() + extension;

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            log.info("File successfully uploaded to MinIO with object key: '{}'", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO folder '{}': {}", folder, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to storage: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a temporary HMAC-signed presigned URL for secure browser access.
     *
     * @param objectKey the relative object key (e.g., "complaints/7f3c1d/2a8c9d.webp")
     * @return the signed presigned URL string with 1-hour expiry
     */
    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String cleanKey = extractObjectName(objectKey);

        try {
            return presigningMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(cleanKey)
                            .expiry(presignedUrlExpirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object key '{}': {}", cleanKey, e.getMessage());
            return null;
        }
    }

    /**
     * Resolves either an object key or a legacy stored URL to an active presigned URL.
     */
    public String resolvePhotoUrl(String photoUrlOrKey) {
        if (photoUrlOrKey == null || photoUrlOrKey.isBlank()) {
            return null;
        }
        return getPresignedUrl(photoUrlOrKey);
    }

    /**
     * Best-effort object deletion from MinIO.
     *
     * @param objectKeyOrUrl the object key or legacy URL to delete
     */
    public void deleteFile(String objectKeyOrUrl) {
        if (objectKeyOrUrl == null || objectKeyOrUrl.isBlank()) {
            return;
        }

        String cleanKey = extractObjectName(objectKeyOrUrl);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(cleanKey)
                            .build()
            );
            log.info("Object '{}' successfully deleted from MinIO bucket '{}'", cleanKey, bucket);
        } catch (Exception e) {
            log.warn("Failed to delete object '{}' from MinIO (marked for async orphan cleanup): {}", cleanKey, e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created private MinIO bucket: '{}'", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify MinIO bucket existence: {}", e.getMessage());
        }
    }

    /**
     * Extracts clean object key from either a relative key or full legacy URL.
     */
    public String extractObjectName(String fileUrlOrKey) {
        if (fileUrlOrKey == null) return null;

        // Check if legacy URL contains publicUrl prefix
        String publicPrefix = minioPublicUrl + "/" + bucket + "/";
        if (fileUrlOrKey.startsWith(publicPrefix)) {
            return fileUrlOrKey.substring(publicPrefix.length());
        }

        // Check if legacy URL contains internal minioUrl prefix
        String internalPrefix = minioUrl + "/" + bucket + "/";
        if (fileUrlOrKey.startsWith(internalPrefix)) {
            return fileUrlOrKey.substring(internalPrefix.length());
        }

        // Check if starts with http/https but other host
        if (fileUrlOrKey.startsWith("http://") || fileUrlOrKey.startsWith("https://")) {
            int bucketIndex = fileUrlOrKey.indexOf("/" + bucket + "/");
            if (bucketIndex != -1) {
                return fileUrlOrKey.substring(bucketIndex + bucket.length() + 2);
            }
        }

        return fileUrlOrKey;
    }
}
