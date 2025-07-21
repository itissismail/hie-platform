package com.hie.platform.shared.minio.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FileUploadResult {
    private String minioPath;
    private String s3Location;
    private String bucketName;
    private String correlationId;
    private String organizationId;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}