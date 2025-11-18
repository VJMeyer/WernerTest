package de.kisters.hmt.cloud.services.upload.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@RedisHash("UploadStatus")
public class UploadStatus implements Serializable {

    @Id
    private String uploadId;

    private String filename;
    private Long fileSize;
    private Long bytesUploaded;
    private String status; // UPLOADING, PROCESSING, COMPLETED, FAILED
    private String filePath;
    private String checksum;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private Double progressPercentage;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttl; // Time to live in seconds

    public UploadStatus() {
    }

    public UploadStatus(String uploadId, String filename, Long fileSize) {
        this.uploadId = uploadId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.bytesUploaded = 0L;
        this.status = "UPLOADING";
        this.createdAt = LocalDateTime.now();
        this.progressPercentage = 0.0;
        this.ttl = null; // No expiration while uploading
    }

    public void updateProgress(Long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
        if (this.fileSize != null && this.fileSize > 0) {
            this.progressPercentage = (bytesUploaded.doubleValue() / fileSize.doubleValue()) * 100.0;
        }
    }

    public void markCompleted(String filePath, String checksum) {
        this.status = "COMPLETED";
        this.filePath = filePath;
        this.checksum = checksum;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100.0;
        this.ttl = 86400L; // 24 hours after completion
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.ttl = 3600L; // 1 hour for failed uploads
    }

    // Getters and Setters

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(Long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "UploadStatus{" +
                "uploadId='" + uploadId + '\'' +
                ", filename='" + filename + '\'' +
                ", fileSize=" + fileSize +
                ", bytesUploaded=" + bytesUploaded +
                ", status='" + status + '\'' +
                ", progressPercentage=" + progressPercentage +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
