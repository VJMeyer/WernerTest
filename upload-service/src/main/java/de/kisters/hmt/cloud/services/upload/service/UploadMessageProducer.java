package de.kisters.hmt.cloud.services.upload.service;

import de.kisters.hmt.cloud.messaging.RabbitMQConfig;
import de.kisters.hmt.cloud.messaging.UploadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UploadMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(UploadMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public UploadMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendUploadNotification(FileStorageService.StoredFile storedFile, String uploaderId) {
        UploadMessage message = new UploadMessage(
                storedFile.getFileId(),
                storedFile.getStoredFileName(),
                storedFile.getOriginalFileName(),
                storedFile.getFileSize(),
                storedFile.getContentType(),
                storedFile.getStoragePath(),
                LocalDateTime.now(),
                uploaderId
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.UPLOAD_EXCHANGE,
                RabbitMQConfig.UPLOAD_ROUTING_KEY,
                message
        );

        logger.info("Sent upload notification for file: {} (ID: {})",
                    storedFile.getOriginalFileName(), storedFile.getFileId());
    }
}
