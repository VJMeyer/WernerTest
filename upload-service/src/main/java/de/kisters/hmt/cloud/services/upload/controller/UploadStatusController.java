package de.kisters.hmt.cloud.services.upload.controller;

import de.kisters.hmt.cloud.services.upload.model.UploadStatus;
import de.kisters.hmt.cloud.services.upload.security.JwtUserInfo;
import de.kisters.hmt.cloud.services.upload.service.UploadStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     *
     * Access Control:
     * - Users can only see their own uploads
     * - Admins can see all uploads in their organization
     * - System admins (ADMIN role without orgId) can see all uploads
     */
    @GetMapping("/{uploadId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ORG_ADMIN')")
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

        // Check access permissions
        if (!canAccessUpload(status)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Access denied");
            errorResponse.put("uploadId", uploadId);
            errorResponse.put("message", "You do not have permission to view this upload");
            return ResponseEntity.status(403).body(errorResponse);
        }
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

        // Include user info for admins
        JwtUserInfo currentUser = JwtUserInfo.fromSecurityContext();
        if (currentUser != null && currentUser.isAdmin()) {
            response.put("uploadedBy", status.getUsername());
            response.put("organization", status.getOrganization());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Check if the current user can access the upload.
     *
     * Access rules:
     * 1. System admins (ADMIN role without orgId) can access all uploads
     * 2. Organization admins can access all uploads in their organization
     * 3. Regular users can only access their own uploads
     */
    private boolean canAccessUpload(UploadStatus upload) {
        JwtUserInfo currentUser = JwtUserInfo.fromSecurityContext();

        if (currentUser == null) {
            return false; // No authentication
        }

        // System admin (no orgId) can see everything
        if (currentUser.hasRole("ADMIN") && currentUser.getOrgId() == null) {
            return true;
        }

        // Must be same organization
        if (upload.getOrgId() != null && !upload.getOrgId().equals(currentUser.getOrgId())) {
            return false;
        }

        // Organization admin can see all uploads in their org
        if (currentUser.hasRole("ADMIN") || currentUser.hasRole("ORG_ADMIN")) {
            return true;
        }

        // Regular user can only see their own uploads
        return upload.getUsername() != null && upload.getUsername().equals(currentUser.getUsername());
    }
}
