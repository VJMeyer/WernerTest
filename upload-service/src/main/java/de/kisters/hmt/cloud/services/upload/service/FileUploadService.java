package de.kisters.hmt.cloud.services.upload.service;

import de.kisters.hmt.cloud.messaging.dto.FileUploadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Value("${rabbitmq.exchange.file-processing}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key.file-upload}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    public FileUploadService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String saveFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("File saved to disk: {}", filePath);

        // Calculate checksum
        String checksum = calculateChecksum(file);

        // Send message to RabbitMQ
        FileUploadMessage message = new FileUploadMessage(
                uniqueFilename,
                originalFilename,
                filePath.toString(),
                file.getSize(),
                file.getContentType(),
                checksum
        );

        sendMessage(message);

        return filePath.toString();
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

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Failed to calculate checksum: {}", e.getMessage());
            return "N/A";
        }
    }
}
