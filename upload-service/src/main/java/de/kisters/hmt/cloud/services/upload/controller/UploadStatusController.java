package de.kisters.hmt.cloud.services.upload.controller;

import de.kisters.hmt.cloud.services.upload.model.UploadStatus;
import de.kisters.hmt.cloud.services.upload.service.UploadStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/upload/status")
public class UploadStatusController {

    private final UploadStatusService uploadStatusService;

    public UploadStatusController(UploadStatusService uploadStatusService) {
        this.uploadStatusService = uploadStatusService;
    }

    /**
     * Get upload status by upload ID
     * This endpoint can be called from any of the 3 servers as Redis is shared
     */
    @GetMapping("/{uploadId}")
    public ResponseEntity<Map<String, Object>> getUploadStatus(@PathVariable String uploadId) {
        Optional<UploadStatus> optionalStatus = uploadStatusService.getUploadStatus(uploadId);

        if (optionalStatus.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Upload not found");
            errorResponse.put("uploadId", uploadId);
            errorResponse.put("message", "Upload status not found or has expired");
            return ResponseEntity.status(404).body(errorResponse);
        }

        UploadStatus status = optionalStatus.get();
        Map<String, Object> response = new HashMap<>();
        response.put("uploadId", status.getUploadId());
        response.put("filename", status.getFilename());
        response.put("fileSize", status.getFileSize());
        response.put("bytesUploaded", status.getBytesUploaded());
        response.put("status", status.getStatus());
        response.put("progressPercentage", String.format("%.2f", status.getProgressPercentage()));
        response.put("createdAt", status.getCreatedAt());

        if (status.getCompletedAt() != null) {
            response.put("completedAt", status.getCompletedAt());
        }

        if (status.getFilePath() != null) {
            response.put("filePath", status.getFilePath());
        }

        if (status.getChecksum() != null) {
            response.put("checksum", status.getChecksum());
        }

        if (status.getErrorMessage() != null) {
            response.put("errorMessage", status.getErrorMessage());
        }

        // Additional metadata
        response.put("isComplete", "COMPLETED".equals(status.getStatus()));
        response.put("isFailed", "FAILED".equals(status.getStatus()));
        response.put("isProcessing", "PROCESSING".equals(status.getStatus()) || "UPLOADING".equals(status.getStatus()));

        return ResponseEntity.ok(response);
    }
}
