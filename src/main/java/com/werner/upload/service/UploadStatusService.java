package com.werner.upload.service;

import com.werner.upload.model.UploadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadStatusService {

    private static final String UPLOAD_STATUS_KEY_PREFIX = "upload:status:";

    private final RedisTemplate<String, UploadStatus> uploadStatusRedisTemplate;

    @Value("${upload.status-ttl-seconds:86400}")
    private long statusTtlSeconds;

    @Value("${server.port:8080}")
    private int serverPort;

    private String getServerId() {
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            return hostname + ":" + serverPort;
        } catch (Exception e) {
            return "unknown:" + serverPort;
        }
    }

    public String generateUploadId() {
        return UUID.randomUUID().toString();
    }

    public UploadStatus createUploadStatus(String uploadId, String fileName, long fileSize) {
        Instant now = Instant.now();
        UploadStatus status = UploadStatus.builder()
                .uploadId(uploadId)
                .fileName(fileName)
                .fileSize(fileSize)
                .bytesTransferred(0)
                .state(UploadStatus.UploadState.PENDING)
                .message("Upload initiated")
                .createdAt(now)
                .updatedAt(now)
                .serverId(getServerId())
                .build();

        saveStatus(status);
        log.info("Created upload status for uploadId: {}", uploadId);
        return status;
    }

    public void updateProgress(String uploadId, long bytesTransferred) {
        Optional<UploadStatus> statusOpt = getStatus(uploadId);
        if (statusOpt.isPresent()) {
            UploadStatus status = statusOpt.get();
            status.setBytesTransferred(bytesTransferred);
            status.setState(UploadStatus.UploadState.UPLOADING);
            status.setMessage("Uploading... " + status.getProgressPercentage() + "% complete");
            status.setUpdatedAt(Instant.now());
            saveStatus(status);
            log.debug("Updated progress for uploadId: {} - {}%", uploadId, status.getProgressPercentage());
        }
    }

    public void markAsProcessing(String uploadId) {
        Optional<UploadStatus> statusOpt = getStatus(uploadId);
        if (statusOpt.isPresent()) {
            UploadStatus status = statusOpt.get();
            status.setState(UploadStatus.UploadState.PROCESSING);
            status.setMessage("Processing uploaded file");
            status.setUpdatedAt(Instant.now());
            saveStatus(status);
            log.info("Marked uploadId: {} as processing", uploadId);
        }
    }

    public void markAsCompleted(String uploadId, String storagePath, String downloadUrl) {
        Optional<UploadStatus> statusOpt = getStatus(uploadId);
        if (statusOpt.isPresent()) {
            UploadStatus status = statusOpt.get();
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(statusTtlSeconds);

            status.setState(UploadStatus.UploadState.COMPLETED);
            status.setMessage("Upload completed successfully");
            status.setStoragePath(storagePath);
            status.setDownloadUrl(downloadUrl);
            status.setCompletedAt(now);
            status.setUpdatedAt(now);
            status.setExpiresAt(expiresAt);
            status.setBytesTransferred(status.getFileSize());

            // Save with TTL of 24 hours
            saveStatusWithTtl(status, Duration.ofSeconds(statusTtlSeconds));
            log.info("Marked uploadId: {} as completed, expires at: {}", uploadId, expiresAt);
        }
    }

    public void markAsRetrieved(String uploadId) {
        Optional<UploadStatus> statusOpt = getStatus(uploadId);
        if (statusOpt.isPresent()) {
            UploadStatus status = statusOpt.get();

            // Only set to RETRIEVED if already COMPLETED
            if (status.getState() == UploadStatus.UploadState.COMPLETED) {
                Instant now = Instant.now();
                Instant expiresAt = now.plusSeconds(statusTtlSeconds);

                status.setState(UploadStatus.UploadState.RETRIEVED);
                status.setMessage("File has been retrieved, status expires in 24 hours");
                status.setUpdatedAt(now);
                status.setExpiresAt(expiresAt);

                // Reset TTL to 24 hours from retrieval
                saveStatusWithTtl(status, Duration.ofSeconds(statusTtlSeconds));
                log.info("Marked uploadId: {} as retrieved, expires at: {}", uploadId, expiresAt);
            }
        }
    }

    public void markAsFailed(String uploadId, String errorDetails) {
        Optional<UploadStatus> statusOpt = getStatus(uploadId);
        if (statusOpt.isPresent()) {
            UploadStatus status = statusOpt.get();
            status.setState(UploadStatus.UploadState.FAILED);
            status.setMessage("Upload failed");
            status.setErrorDetails(errorDetails);
            status.setUpdatedAt(Instant.now());

            // Keep failed status for 24 hours as well
            saveStatusWithTtl(status, Duration.ofSeconds(statusTtlSeconds));
            log.error("Marked uploadId: {} as failed: {}", uploadId, errorDetails);
        }
    }

    public Optional<UploadStatus> getStatus(String uploadId) {
        String key = UPLOAD_STATUS_KEY_PREFIX + uploadId;
        UploadStatus status = uploadStatusRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(status);
    }

    private void saveStatus(UploadStatus status) {
        String key = UPLOAD_STATUS_KEY_PREFIX + status.getUploadId();
        uploadStatusRedisTemplate.opsForValue().set(key, status);
    }

    private void saveStatusWithTtl(UploadStatus status, Duration ttl) {
        String key = UPLOAD_STATUS_KEY_PREFIX + status.getUploadId();
        uploadStatusRedisTemplate.opsForValue().set(key, status, ttl);
    }
}
