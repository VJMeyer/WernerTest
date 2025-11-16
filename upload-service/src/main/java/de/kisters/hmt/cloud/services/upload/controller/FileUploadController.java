package de.kisters.hmt.cloud.services.upload.controller;

import de.kisters.hmt.cloud.services.upload.model.UploadResponse;
import de.kisters.hmt.cloud.services.upload.service.FileStorageService;
import de.kisters.hmt.cloud.services.upload.service.UploadMessageProducer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileStorageService fileStorageService;
    private final UploadMessageProducer messageProducer;

    public FileUploadController(FileStorageService fileStorageService,
                                UploadMessageProducer messageProducer) {
        this.fileStorageService = fileStorageService;
        this.messageProducer = messageProducer;
    }

    @PostMapping("/small")
    public ResponseEntity<UploadResponse> uploadSmallFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploaderId", defaultValue = "anonymous") String uploaderId) {

        logger.info("Received small file upload request: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

        try {
            FileStorageService.StoredFile storedFile = fileStorageService.storeFile(file);

            messageProducer.sendUploadNotification(storedFile, uploaderId);

            UploadResponse response = new UploadResponse(
                    storedFile.getFileId(),
                    storedFile.getOriginalFileName(),
                    storedFile.getFileSize(),
                    "File uploaded successfully",
                    true
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Failed to store file: {}", file.getOriginalFilename(), e);
            UploadResponse errorResponse = new UploadResponse(
                    null,
                    file.getOriginalFilename(),
                    file.getSize(),
                    "Failed to upload file: " + e.getMessage(),
                    false
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/large")
    public ResponseEntity<UploadResponse> uploadLargeFile(
            HttpServletRequest request,
            @RequestHeader(value = "X-File-Name", defaultValue = "unknown") String fileName,
            @RequestHeader(value = "Content-Type", defaultValue = "application/octet-stream") String contentType,
            @RequestHeader(value = "Content-Length", defaultValue = "0") long contentLength,
            @RequestHeader(value = "X-Uploader-Id", defaultValue = "anonymous") String uploaderId) {

        logger.info("Received large file upload request: {} (expected size: {} bytes)",
                    fileName, contentLength);

        try {
            FileStorageService.StoredFile storedFile = fileStorageService.storeFileStreaming(
                    request.getInputStream(), fileName, contentType, contentLength);

            messageProducer.sendUploadNotification(storedFile, uploaderId);

            UploadResponse response = new UploadResponse(
                    storedFile.getFileId(),
                    storedFile.getOriginalFileName(),
                    storedFile.getFileSize(),
                    "Large file uploaded successfully",
                    true
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Failed to store large file: {}", fileName, e);
            UploadResponse errorResponse = new UploadResponse(
                    null,
                    fileName,
                    contentLength,
                    "Failed to upload large file: " + e.getMessage(),
                    false
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Upload service is running");
    }
}
