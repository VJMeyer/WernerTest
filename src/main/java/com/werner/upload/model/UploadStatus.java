package com.werner.upload.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatus implements Serializable {

    private String uploadId;
    private String fileName;
    private long fileSize;
    private long bytesTransferred;
    private UploadState state;
    private String message;
    private String errorDetails;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiresAt;

    private String storagePath;
    private String downloadUrl;
    private String serverId;

    public int getProgressPercentage() {
        if (fileSize <= 0) {
            return 0;
        }
        return (int) ((bytesTransferred * 100) / fileSize);
    }

    public enum UploadState {
        PENDING,
        UPLOADING,
        PROCESSING,
        COMPLETED,
        RETRIEVED,
        FAILED,
        EXPIRED
    }
}
