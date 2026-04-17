package com.softy.be.report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class ReportS3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String keyPrefix;
    private final long presignedExpireSeconds;

    public ReportS3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${report.pdf.s3.bucket:}") String bucket,
            @Value("${report.pdf.s3.key-prefix:reports}") String keyPrefix,
            @Value("${report.pdf.s3.presigned-expire-seconds:600}") long presignedExpireSeconds
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
        this.presignedExpireSeconds = presignedExpireSeconds;
    }

    public String uploadPdf(String key, byte[] bytes) {
        validateBucket();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return toS3Uri(key);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 업로드에 실패했습니다.", e);
        }
    }

    public String createDownloadUrl(String key) {
        validateBucket();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .responseContentType("application/pdf")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedExpireSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "다운로드 링크 생성에 실패했습니다.", e);
        }
    }

    public String buildObjectKey(Long chatRoomId, String fileName) {
        String prefix = keyPrefix == null ? "reports" : keyPrefix.trim();
        if (prefix.endsWith("/")) {
            return prefix + "chat-rooms/" + chatRoomId + "/" + fileName;
        }
        return prefix + "/chat-rooms/" + chatRoomId + "/" + fileName;
    }

    public long getPresignedExpireSeconds() {
        return presignedExpireSeconds;
    }

    public String extractKeyFromS3Uri(String s3Uri) {
        String expectedPrefix = "s3://" + bucket + "/";
        if (s3Uri == null || !s3Uri.startsWith(expectedPrefix)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 S3 경로가 올바르지 않습니다.");
        }
        return s3Uri.substring(expectedPrefix.length());
    }

    private String toS3Uri(String key) {
        return "s3://" + bucket + "/" + key;
    }

    private void validateBucket() {
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_PDF_S3_BUCKET 설정이 필요합니다.");
        }
    }
}

