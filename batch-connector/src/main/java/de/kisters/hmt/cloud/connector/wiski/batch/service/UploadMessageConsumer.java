package de.kisters.hmt.cloud.connector.wiski.batch.service;

import de.kisters.hmt.cloud.messaging.RabbitMQConfig;
import de.kisters.hmt.cloud.messaging.UploadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class UploadMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UploadMessageConsumer.class);

    private final BatchFileGenerator batchFileGenerator;
    private final WiskiBatExecutor wiskiBatExecutor;

    public UploadMessageConsumer(BatchFileGenerator batchFileGenerator,
                                  WiskiBatExecutor wiskiBatExecutor) {
        this.batchFileGenerator = batchFileGenerator;
        this.wiskiBatExecutor = wiskiBatExecutor;
    }

    @RabbitListener(queues = RabbitMQConfig.UPLOAD_QUEUE)
    public void handleUploadMessage(UploadMessage message) {
        logger.info("Received upload message: {}", message);

        try {
            // Generate batch file with metadata
            Path batchFilePath = batchFileGenerator.generateBatchFile(message);
            logger.info("Generated batch file: {}", batchFilePath);

            // Execute wiskBat with the batch file
            WiskiBatExecutor.ExecutionResult result = wiskiBatExecutor.executeWiskBat(batchFilePath);

            if (result.isSuccess()) {
                logger.info("Successfully processed upload message for file: {} (ID: {})",
                           message.getOriginalFileName(), message.getFileId());
            } else {
                logger.error("Failed to process upload message for file: {} (ID: {}). Result: {}",
                            message.getOriginalFileName(), message.getFileId(), result);
            }

            // Clean up batch file if configured
            batchFileGenerator.deleteBatchFile(batchFilePath);

        } catch (IOException e) {
            logger.error("Error processing upload message for file: {} (ID: {})",
                        message.getOriginalFileName(), message.getFileId(), e);
            throw new RuntimeException("Failed to process upload message", e);
        }
    }
}
