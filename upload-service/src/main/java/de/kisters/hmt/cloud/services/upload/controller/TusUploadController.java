package de.kisters.hmt.cloud.services.upload.controller;

import de.kisters.hmt.cloud.services.upload.listener.TusUploadListener;
import de.kisters.hmt.cloud.services.upload.service.UploadStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/tus")
public class TusUploadController {

    private static final Logger logger = LoggerFactory.getLogger(TusUploadController.class);

    private final TusFileUploadService tusFileUploadService;
    private final TusUploadListener tusUploadListener;
    private final UploadStatusService uploadStatusService;

    public TusUploadController(TusFileUploadService tusFileUploadService,
                              TusUploadListener tusUploadListener,
                              UploadStatusService uploadStatusService) {
        this.tusFileUploadService = tusFileUploadService;
        this.tusUploadListener = tusUploadListener;
        this.uploadStatusService = uploadStatusService;
    }

    /**
     * Main TUS protocol endpoint - handles all TUS requests (POST, PATCH, HEAD, OPTIONS, DELETE)
     */
    @RequestMapping(value = {"/upload", "/upload/**"}, method = {
            RequestMethod.POST,
            RequestMethod.PATCH,
            RequestMethod.HEAD,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.GET
    })
    public void tusUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("TUS request: {} {}", request.getMethod(), request.getRequestURI());
        String method = request.getMethod();

        try {
            // Process the TUS protocol request
            tusFileUploadService.process(request, response);

            // Extract upload ID from request or response
            String uploadId = getUploadIdFromRequest(request);
            if (uploadId == null && "POST".equals(method)) {
                // For POST, get upload ID from Location header in response
                String location = response.getHeader("Location");
                if (location != null) {
                    uploadId = location.substring(location.lastIndexOf('/') + 1);
                }
            }

            if (uploadId != null) {
                UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadId);

                if (uploadInfo != null) {
                    // Handle POST - create initial upload status
                    if ("POST".equals(method)) {
                        String filename = uploadInfo.getFileName();
                        if (filename == null || filename.isEmpty()) {
                            filename = uploadInfo.getMetadata().get("filename");
                        }
                        Long fileSize = uploadInfo.getLength();
                        uploadStatusService.createUploadStatus(uploadId, filename, fileSize);
                        logger.info("Created upload status for TUS upload ID: {}", uploadId);
                    }

                    // Handle PATCH - update progress
                    if ("PATCH".equals(method)) {
                        Long bytesUploaded = uploadInfo.getOffset();
                        uploadStatusService.updateProgress(uploadId, bytesUploaded);
                        logger.debug("Updated progress for upload {}: {} bytes", uploadId, bytesUploaded);
                    }

                    // Check if upload is complete
                    if (!uploadInfo.isUploadInProgress()) {
                        logger.info("Upload completed for ID: {}", uploadId);
                        // Process the completed upload asynchronously
                        processCompletedUploadAsync(uploadId);
                    }
                }
            }

        } catch (TusException e) {
            logger.error("TUS protocol error: {}", e.getMessage(), e);
            String uploadId = getUploadIdFromRequest(request);
            if (uploadId != null) {
                uploadStatusService.markFailed(uploadId, e.getMessage());
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO error during TUS upload: {}", e.getMessage(), e);
            String uploadId = getUploadIdFromRequest(request);
            if (uploadId != null) {
                uploadStatusService.markFailed(uploadId, e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Health check endpoint for TUS service
     */
    @GetMapping("/health")
    public String tusHealth() {
        return "TUS upload service is running";
    }

    /**
     * Extract upload ID from request URI
     */
    private String getUploadIdFromRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String tusPath = "/api/tus/upload/";

        if (requestURI.contains(tusPath)) {
            String[] parts = requestURI.split(tusPath);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                return parts[1];
            }
        }

        return null;
    }

    /**
     * Process completed upload asynchronously to avoid blocking the TUS response
     */
    private void processCompletedUploadAsync(String uploadId) {
        // Using virtual threads, we can simply spawn a new thread for async processing
        Thread.ofVirtual().start(() -> {
            try {
                tusUploadListener.processCompletedUpload(uploadId);
            } catch (Exception e) {
                logger.error("Error processing completed upload {}: {}", uploadId, e.getMessage(), e);
            }
        });
    }
}
