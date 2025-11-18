package de.kisters.hmt.cloud.services.upload.controller.web;

import de.kisters.hmt.cloud.services.upload.model.UploadStatus;
import de.kisters.hmt.cloud.services.upload.security.JwtUserInfo;
import de.kisters.hmt.cloud.services.upload.service.FileUploadService;
import de.kisters.hmt.cloud.services.upload.service.UploadStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

/**
 * HTMX endpoints for dynamic file upload and status monitoring.
 * Returns HTML fragments that HTMX swaps into the page.
 */
@Controller
@RequestMapping("/htmx")
public class UploadWebController {

    private static final Logger logger = LoggerFactory.getLogger(UploadWebController.class);

    private final FileUploadService fileUploadService;
    private final UploadStatusService uploadStatusService;

    public UploadWebController(FileUploadService fileUploadService,
                               UploadStatusService uploadStatusService) {
        this.fileUploadService = fileUploadService;
        this.uploadStatusService = uploadStatusService;
    }

    /**
     * Handle file upload via HTMX.
     * Returns HTML fragment with upload result.
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();

        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload");
                return "fragments/upload-error :: error";
            }

            logger.info("Processing upload from user {}: {} ({} bytes)",
                    userInfo.getUsername(), file.getOriginalFilename(), file.getSize());

            Map<String, Object> uploadResult = fileUploadService.saveFile(file);

            model.addAttribute("uploadId", uploadResult.get("uploadId"));
            model.addAttribute("filename", file.getOriginalFilename());
            model.addAttribute("size", file.getSize());
            model.addAttribute("statusUrl", "/htmx/upload-status/" + uploadResult.get("uploadId"));

            return "fragments/upload-success :: success";

        } catch (Exception e) {
            logger.error("Upload failed for user {}: {}", userInfo.getUsername(), e.getMessage(), e);
            model.addAttribute("error", "Upload failed: " + e.getMessage());
            return "fragments/upload-error :: error";
        }
    }

    /**
     * Get upload status fragment via HTMX polling.
     * Returns HTML fragment with current upload status.
     */
    @GetMapping("/upload-status/{uploadId}")
    public String getUploadStatus(@PathVariable String uploadId, Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();
        Optional<UploadStatus> optionalStatus = uploadStatusService.getUploadStatus(uploadId);

        if (optionalStatus.isEmpty()) {
            model.addAttribute("error", "Upload not found");
            model.addAttribute("uploadId", uploadId);
            return "fragments/upload-error :: error";
        }

        UploadStatus status = optionalStatus.get();

        // Check access permissions
        if (!canAccessUpload(status, userInfo)) {
            model.addAttribute("error", "Access denied");
            return "fragments/upload-error :: error";
        }

        model.addAttribute("status", status);
        model.addAttribute("uploadId", status.getUploadId());
        model.addAttribute("filename", status.getFilename());
        model.addAttribute("fileSize", formatFileSize(status.getFileSize()));
        model.addAttribute("progress", String.format("%.0f", status.getProgressPercentage()));
        model.addAttribute("statusText", status.getStatus());
        model.addAttribute("isComplete", "COMPLETED".equals(status.getStatus()));
        model.addAttribute("isFailed", "FAILED".equals(status.getStatus()));
        model.addAttribute("isProcessing", "PROCESSING".equals(status.getStatus()) || "UPLOADING".equals(status.getStatus()));

        if (userInfo.isAdmin()) {
            model.addAttribute("showAdminInfo", true);
            model.addAttribute("uploadedBy", status.getUsername());
            model.addAttribute("organization", status.getOrganization());
        }

        return "fragments/upload-status :: status";
    }

    /**
     * Get list of recent uploads for current user.
     */
    @GetMapping("/my-uploads")
    public String getMyUploads(Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();
        // In a real implementation, you'd query uploads from a repository
        // For now, this is a placeholder
        model.addAttribute("uploads", java.util.Collections.emptyList());
        return "fragments/upload-list :: list";
    }

    /**
     * Check if user can access upload.
     */
    private boolean canAccessUpload(UploadStatus upload, JwtUserInfo user) {
        if (user == null) {
            return false;
        }

        // System admin can see everything
        if (user.hasRole("ADMIN") && user.getOrgId() == null) {
            return true;
        }

        // Must be same organization
        if (upload.getOrgId() != null && !upload.getOrgId().equals(user.getOrgId())) {
            return false;
        }

        // Organization admin can see all in their org
        if (user.hasRole("ADMIN") || user.hasRole("ORG_ADMIN")) {
            return true;
        }

        // Regular user can only see their own
        return upload.getUsername() != null && upload.getUsername().equals(user.getUsername());
    }

    /**
     * Format file size in human-readable format.
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null) {
            return "0 B";
        }

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
