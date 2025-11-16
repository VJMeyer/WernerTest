package de.kisters.hmt.cloud.services.upload.model;

public class UploadResponse {

    private String fileId;
    private String fileName;
    private long fileSize;
    private String message;
    private boolean success;

    public UploadResponse() {
    }

    public UploadResponse(String fileId, String fileName, long fileSize, String message, boolean success) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.message = message;
        this.success = success;
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
