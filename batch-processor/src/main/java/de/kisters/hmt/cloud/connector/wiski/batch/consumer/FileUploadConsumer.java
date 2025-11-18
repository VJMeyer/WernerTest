package de.kisters.hmt.cloud.connector.wiski.batch.consumer;

import de.kisters.hmt.cloud.connector.wiski.batch.service.BatchFileService;
import de.kisters.hmt.cloud.messaging.dto.FileUploadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileUploadConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadConsumer.class);

    private final BatchFileService batchFileService;

    @Autowired
    public FileUploadConsumer(BatchFileService batchFileService) {
        this.batchFileService = batchFileService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.file-upload}")
    public void consumeFileUploadMessage(FileUploadMessage message) {
        logger.info("Received file upload message: {}", message);

        try {
            // Process the file upload message
            batchFileService.processFileUpload(message);
            logger.info("Successfully processed file upload message for: {}", message.getOriginalFilename());

        } catch (Exception e) {
            logger.error("Error processing file upload message: {}", e.getMessage(), e);
            // In production, you might want to send to a dead letter queue
        }
    }
}
