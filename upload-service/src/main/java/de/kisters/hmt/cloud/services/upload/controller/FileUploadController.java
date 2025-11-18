package de.kisters.hmt.cloud.services.upload.controller;

import de.kisters.hmt.cloud.services.upload.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Received file upload request: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

            Map<String, Object> uploadResult = fileUploadService.saveFile(file);

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("uploadId", uploadResult.get("uploadId"));
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("path", uploadResult.get("filePath"));
            response.put("checksum", uploadResult.get("checksum"));
            response.put("statusUrl", "/api/upload/status/" + uploadResult.get("uploadId"));

            logger.info("File uploaded successfully: {}", uploadResult.get("filePath"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "upload-service");
        return ResponseEntity.ok(response);
    }
}
