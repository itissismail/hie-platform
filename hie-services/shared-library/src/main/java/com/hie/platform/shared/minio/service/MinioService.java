package com.hie.platform.shared.minio.service;

import com.hie.platform.shared.minio.config.MinioProperties;
import com.hie.platform.shared.minio.model.FileUploadResult;
import com.hie.platform.shared.minio.exception.MinioServiceException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Autowired
    public MinioService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * Upload file with structured path: organization-id/year/month/day/correlationId.ext
     */
    public Mono<FileUploadResult> uploadFile(String content, String organizationId,
                                             String correlationId, String fileExtension, String minioPath) {
        return uploadFile(content, organizationId, correlationId, fileExtension, "text/plain", minioPath);
    }

    /**
     * Upload file with structured path and custom content type
     */
    public Mono<FileUploadResult> uploadFile(String content, String organizationId,
                                             String messageId, String fileExtension, String contentType, String minioPath) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Attempting to upload content of size: {} bytes", content.length());

                ensureBucketExists();
                log.info("Bucket check/creation complete for bucket: {}", minioProperties.getBucketName());

                // Convert content to bytes
                byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

                // Upload file
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(minioPath)
                                .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                                .contentType(contentType)
                                .build()
                );

                String s3Location = String.format("s3://%s/%s", minioProperties.getBucketName(), minioPath);

                log.debug("File uploaded successfully to MinIO: {}", s3Location);

                return FileUploadResult.builder()
                        .minioPath(minioPath)
                        .s3Location(s3Location)
                        .bucketName(minioProperties.getBucketName())
                        .messageId(messageId)
                        .organizationId(organizationId)
                        .fileSize((long) contentBytes.length)
                        .contentType(contentType)
                        .uploadedAt(LocalDateTime.now())
                        .build();

            } catch (Exception e) {
                log.error("Error uploading file to MinIO for messageId: {}", messageId, e);
                throw new MinioServiceException("Failed to upload file to MinIO", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Upload binary file
     */
    public Mono<FileUploadResult> uploadFile(byte[] content, String organizationId,
                                             String messageId, String fileExtension, String contentType, String minioPath) {
        return Mono.fromCallable(() -> {
            try {
                //String minioPath = generateStructuredPath(organizationId, messageId, fileExtension);
                ensureBucketExists();

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(minioPath)
                                .stream(new ByteArrayInputStream(content), content.length, -1)
                                .contentType(contentType)
                                .build()
                );

                String s3Location = String.format("s3://%s/%s", minioProperties.getBucketName(), minioPath);

                return FileUploadResult.builder()
                        .minioPath(minioPath)
                        .s3Location(s3Location)
                        .bucketName(minioProperties.getBucketName())
                        .messageId(messageId)
                        .organizationId(organizationId)
                        .fileSize((long) content.length)
                        .contentType(contentType)
                        .uploadedAt(LocalDateTime.now())
                        .build();

            } catch (Exception e) {
                throw new MinioServiceException("Failed to upload binary file to MinIO", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Download file content as string
     */
    public Mono<String> downloadFileAsString(String minioPath) {
        return Mono.fromCallable(() -> {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(minioPath)
                            .build())) {

                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Failed to download file as string from MinIO path: {}", minioPath, e);
                throw new MinioServiceException("Failed to download file from MinIO: " + minioPath, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Download file as byte array
     */
    public Mono<byte[]> downloadFileAsBytes(String minioPath) {
        return Mono.fromCallable(() -> {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(minioPath)
                            .build())) {

                return stream.readAllBytes();
            } catch (Exception e) {
                log.error("Failed to download file as bytes from MinIO path: {}", minioPath, e);
                throw new MinioServiceException("Failed to download file from MinIO: " + minioPath, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Download file synchronously - for use with existing blocking code
     *
     * @param minioPath Path to the file in MinIO
     * @return File content as byte array
     * @throws MinioServiceException if download fails
     */
    public byte[] downloadFile(String minioPath) {
        try {
            log.debug("Downloading file from MinIO path: {}", minioPath);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(minioPath)
                            .build())) {

                byte[] content = stream.readAllBytes();
                log.debug("Successfully downloaded file - Size: {} bytes", content.length);
                return content;

            } catch (Exception e) {
                log.error("Failed to download file from MinIO path: {}", minioPath, e);
                throw new MinioServiceException("Failed to download file from MinIO: " + minioPath, e);
            }
        } catch (Exception e) {
            log.error("Error during file download from MinIO path: {}", minioPath, e);
            throw new MinioServiceException("Failed to download file from MinIO: " + minioPath, e);
        }
    }

    /**
     * Download file synchronously as string
     *
     * @param minioPath Path to the file in MinIO
     * @return File content as UTF-8 string
     * @throws MinioServiceException if download fails
     */
    public String downloadFileAsStringSync(String minioPath) {
        try {
            byte[] content = downloadFile(minioPath);
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to download file as string from MinIO path: {}", minioPath, e);
            throw new MinioServiceException("Failed to download file as string from MinIO: " + minioPath, e);
        }
    }


    /**
     * Delete file
     */
    public Mono<Boolean> deleteFile(String minioPath) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(minioPath)
                                .build()
                );
                log.debug("File deleted successfully from MinIO: {}", minioPath);
                return true;
            } catch (Exception e) {
                log.error("Failed to delete file from MinIO: {}", minioPath, e);
                throw new MinioServiceException("Failed to delete file from MinIO: " + minioPath, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Check if file exists
     */
    public Mono<Boolean> fileExists(String minioPath) {
        return Mono.fromCallable(() -> {
            try {
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(minioPath)
                                .build()
                );
                return stat != null;
            } catch (Exception e) {
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * Ensure bucket exists, create if it doesn't
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );
            log.info("Created MinIO bucket: {}", minioProperties.getBucketName());
        }
    }

}