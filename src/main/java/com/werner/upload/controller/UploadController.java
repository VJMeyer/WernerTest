package com.werner.upload.controller;

import com.werner.upload.model.UploadResponse;
import com.werner.upload.model.UploadStatus;
import com.werner.upload.service.UploadService;
import com.werner.upload.service.UploadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final UploadService uploadService;
    private final UploadStatusService statusService;

    /**
     * Upload a file and receive a monitoring URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(UploadResponse.builder()
                            .message("No file provided")
                            .build());
        }

        log.info("Received upload request for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        UploadResponse response = uploadService.uploadFile(file);

        if (response.getStatus() != null &&
                response.getStatus().getState() == UploadStatus.UploadState.FAILED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get upload status - the monitoring endpoint
     * This endpoint can be called from any of the 3 servers
     */
    @GetMapping("/{uploadId}/status")
    public ResponseEntity<?> getUploadStatus(@PathVariable String uploadId) {
        log.debug("Status check requested for uploadId: {}", uploadId);

        Optional<UploadStatus> statusOpt = statusService.getStatus(uploadId);

        if (statusOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Upload not found",
                            "uploadId", uploadId,
                            "message", "Upload ID not found or status has expired"
                    ));
        }

        return ResponseEntity.ok(statusOpt.get());
    }

    /**
     * Download the uploaded file
     * Also marks the upload as retrieved and starts the 24-hour TTL
     */
    @GetMapping("/{uploadId}/download")
    public ResponseEntity<?> downloadFile(@PathVariable String uploadId) {
        log.info("Download requested for uploadId: {}", uploadId);

        Optional<UploadStatus> statusOpt = statusService.getStatus(uploadId);

        if (statusOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Upload not found",
                            "uploadId", uploadId,
                            "message", "Upload ID not found or status has expired"
                    ));
        }

        UploadStatus status = statusOpt.get();

        if (status.getState() != UploadStatus.UploadState.COMPLETED &&
                status.getState() != UploadStatus.UploadState.RETRIEVED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "File not ready for download",
                            "uploadId", uploadId,
                            "currentState", status.getState().toString(),
                            "message", "File upload is not yet completed"
                    ));
        }

        try {
            Path filePath = Paths.get(status.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "error", "File not accessible",
                                "uploadId", uploadId,
                                "message", "The file exists in status but is not accessible on storage"
                        ));
            }

            // Mark as retrieved - this starts/resets the 24-hour TTL
            statusService.markAsRetrieved(uploadId);

            String contentType = "application/octet-stream";
            String filename = status.getFileName();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Error creating resource for uploadId: {}", uploadId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal error",
                            "uploadId", uploadId,
                            "message", "Error accessing file: " + e.getMessage()
                    ));
        }
    }

    /**
     * Health check endpoint for load balancer
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "upload-service"
        ));
    }
}
