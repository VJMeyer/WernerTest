package com.werner.upload.service;

import com.werner.upload.config.RabbitMQConfig;
import com.werner.upload.model.UploadResponse;
import com.werner.upload.model.UploadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final UploadStatusService statusService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${upload.storage-path:/mnt/nfs/uploads}")
    private String storagePath;

    @Value("${upload.base-url:http://localhost:8080}")
    private String baseUrl;

    public UploadResponse uploadFile(MultipartFile file) {
        String uploadId = statusService.generateUploadId();
        String originalFilename = file.getOriginalFilename() != null ?
                file.getOriginalFilename() : "unknown";
        long fileSize = file.getSize();

        // Create initial status
        UploadStatus status = statusService.createUploadStatus(uploadId, originalFilename, fileSize);

        // Generate monitoring URL
        String monitoringUrl = generateMonitoringUrl(uploadId);

        // Publish upload started event
        publishEvent("upload.status.started", status);

        try {
            // Process the upload with progress tracking
            String savedPath = processUpload(uploadId, file);

            // Generate download URL
            String downloadUrl = generateDownloadUrl(uploadId, originalFilename);

            // Mark as completed
            statusService.markAsCompleted(uploadId, savedPath, downloadUrl);

            // Publish completion event
            UploadStatus completedStatus = statusService.getStatus(uploadId).orElse(status);
            publishEvent("upload.status.completed", completedStatus);

            return UploadResponse.builder()
                    .uploadId(uploadId)
                    .monitoringUrl(monitoringUrl)
                    .message("File uploaded successfully. Monitor status at: " + monitoringUrl)
                    .status(completedStatus)
                    .build();

        } catch (Exception e) {
            log.error("Upload failed for uploadId: {}", uploadId, e);
            statusService.markAsFailed(uploadId, e.getMessage());

            UploadStatus failedStatus = statusService.getStatus(uploadId).orElse(status);
            publishEvent("upload.status.failed", failedStatus);

            return UploadResponse.builder()
                    .uploadId(uploadId)
                    .monitoringUrl(monitoringUrl)
                    .message("Upload failed: " + e.getMessage())
                    .status(failedStatus)
                    .build();
        }
    }

    private String processUpload(String uploadId, MultipartFile file) throws IOException {
        statusService.markAsProcessing(uploadId);

        // Create date-based directory structure
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadDir = Paths.get(storagePath, dateDir, uploadId);
        Files.createDirectories(uploadDir);

        // Save file with original name
        String originalFilename = file.getOriginalFilename() != null ?
                file.getOriginalFilename() : "file";
        Path filePath = uploadDir.resolve(originalFilename);

        // Stream file with progress updates
        long totalBytes = file.getSize();
        long bytesWritten = 0;
        int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];
        int lastReportedPercentage = 0;

        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = Files.newOutputStream(filePath)) {

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;

                // Update progress every 10%
                int currentPercentage = (int) ((bytesWritten * 100) / totalBytes);
                if (currentPercentage >= lastReportedPercentage + 10) {
                    statusService.updateProgress(uploadId, bytesWritten);
                    lastReportedPercentage = currentPercentage;
                }
            }
        }

        // Final progress update
        statusService.updateProgress(uploadId, totalBytes);

        log.info("File saved to: {}", filePath);
        return filePath.toString();
    }

    private String generateMonitoringUrl(String uploadId) {
        return baseUrl + "/api/uploads/" + uploadId + "/status";
    }

    private String generateDownloadUrl(String uploadId, String filename) {
        return baseUrl + "/api/uploads/" + uploadId + "/download";
    }

    private void publishEvent(String routingKey, UploadStatus status) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.UPLOAD_EVENTS_EXCHANGE,
                    routingKey,
                    status
            );
            log.debug("Published event {} for uploadId: {}", routingKey, status.getUploadId());
        } catch (Exception e) {
            log.warn("Failed to publish event {} for uploadId: {}", routingKey, status.getUploadId(), e);
            // Don't fail the upload if event publishing fails
        }
    }
}
