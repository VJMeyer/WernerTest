package de.kisters.hmt.cloud.messaging.dto;

import java.time.LocalDateTime;

public class FileUploadMessage {

    private String filename;
    private String originalFilename;
    private String filePath;
    private long fileSize;
    private String contentType;
    private LocalDateTime uploadTimestamp;
    private String checksum;

    public FileUploadMessage() {
    }

    public FileUploadMessage(String filename, String originalFilename, String filePath,
                            long fileSize, String contentType, String checksum) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadTimestamp = LocalDateTime.now();
        this.checksum = checksum;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return "FileUploadMessage{" +
                "filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", uploadTimestamp=" + uploadTimestamp +
                ", checksum='" + checksum + '\'' +
                '}';
    }
}
