package de.kisters.hmt.cloud.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UploadMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileId;
    private String fileName;
    private String originalFileName;
    private long fileSize;
    private String contentType;
    private String storagePath;
    private LocalDateTime uploadTimestamp;
    private String uploaderId;

    public UploadMessage() {
    }

    public UploadMessage(String fileId, String fileName, String originalFileName,
                         long fileSize, String contentType, String storagePath,
                         LocalDateTime uploadTimestamp, String uploaderId) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.uploadTimestamp = uploadTimestamp;
        this.uploaderId = uploaderId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    @Override
    public String toString() {
        return "UploadMessage{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", storagePath='" + storagePath + '\'' +
                ", uploadTimestamp=" + uploadTimestamp +
                ", uploaderId='" + uploaderId + '\'' +
                '}';
    }
}
