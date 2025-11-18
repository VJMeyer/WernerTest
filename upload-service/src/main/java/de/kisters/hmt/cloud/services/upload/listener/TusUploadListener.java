package de.kisters.hmt.cloud.services.upload.listener;

import de.kisters.hmt.cloud.messaging.dto.FileUploadMessage;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class TusUploadListener {

    private static final Logger logger = LoggerFactory.getLogger(TusUploadListener.class);

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Value("${rabbitmq.exchange.file-processing}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key.file-upload}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;
    private final TusFileUploadService tusFileUploadService;

    public TusUploadListener(RabbitTemplate rabbitTemplate, TusFileUploadService tusFileUploadService) {
        this.rabbitTemplate = rabbitTemplate;
        this.tusFileUploadService = tusFileUploadService;
    }

    public void processCompletedUpload(String uploadId) throws IOException {
        logger.info("Processing completed TUS upload: {}", uploadId);

        // Get upload information
        UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadId);

        if (uploadInfo == null) {
            logger.error("Upload info not found for upload ID: {}", uploadId);
            return;
        }

        // Extract metadata
        String originalFilename = uploadInfo.getFileName();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = uploadInfo.getMetadata().get("filename");
        }
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "uploaded_file";
        }

        Long fileSize = uploadInfo.getLength();
        String contentType = uploadInfo.getFileMimeType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = uploadInfo.getMetadata().get("filetype");
        }
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        // Generate unique filename for permanent storage
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Move file from tus storage to permanent storage
        Path permanentFilePath = uploadPath.resolve(uniqueFilename);

        try (InputStream uploadedStream = tusFileUploadService.getUploadedBytes(uploadId)) {
            Files.copy(uploadedStream, permanentFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File moved to permanent storage: {}", permanentFilePath);
        }

        // Calculate checksum
        String checksum = calculateChecksum(permanentFilePath);

        // Create and send message to RabbitMQ
        FileUploadMessage message = new FileUploadMessage(
                uniqueFilename,
                originalFilename,
                permanentFilePath.toString(),
                fileSize != null ? fileSize : 0L,
                contentType,
                checksum
        );

        sendMessage(message);

        // Clean up tus upload data
        try {
            tusFileUploadService.deleteUpload(uploadId);
            logger.info("Cleaned up TUS upload data for: {}", uploadId);
        } catch (Exception e) {
            logger.warn("Failed to clean up TUS upload data: {}", e.getMessage());
        }

        logger.info("Successfully processed upload: {} -> {}", originalFilename, permanentFilePath);
    }

    private void sendMessage(FileUploadMessage message) {
        try {
            logger.info("Sending file upload message to RabbitMQ: {}", message);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            logger.info("Message sent successfully to exchange: {}, routing key: {}", exchangeName, routingKey);
        } catch (Exception e) {
            logger.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }

    private String calculateChecksum(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Failed to calculate checksum: {}", e.getMessage());
            return "N/A";
        }
    }
}
