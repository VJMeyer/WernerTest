package de.kisters.hmt.cloud.connector.wiski.batch.service;

import de.kisters.hmt.cloud.connector.wiski.batch.config.WiskiBatConfig;
import de.kisters.hmt.cloud.messaging.UploadMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
public class BatchFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BatchFileGenerator.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final WiskiBatConfig config;
    private Path batchFileDirectory;

    public BatchFileGenerator(WiskiBatConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        try {
            batchFileDirectory = Paths.get(config.getBatchFileDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(batchFileDirectory);
            logger.info("Batch file directory initialized: {}", batchFileDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create batch file directory", e);
        }
    }

    public Path generateBatchFile(UploadMessage message) throws IOException {
        String timestamp = message.getUploadTimestamp().format(TIMESTAMP_FORMATTER);
        String batchFileName = String.format("wiski_import_%s_%s.bat", message.getFileId(), timestamp);

        Path batchFilePath = batchFileDirectory.resolve(batchFileName);

        StringBuilder batchContent = new StringBuilder();
        batchContent.append("@echo off\n");
        batchContent.append("REM WISKI Import Batch File\n");
        batchContent.append("REM Generated for file upload: ").append(message.getFileId()).append("\n");
        batchContent.append("REM Timestamp: ").append(message.getUploadTimestamp()).append("\n");
        batchContent.append("\n");
        batchContent.append("REM === File Metadata ===\n");
        batchContent.append("SET FILE_ID=").append(message.getFileId()).append("\n");
        batchContent.append("SET FILE_NAME=").append(escapeForBatch(message.getFileName())).append("\n");
        batchContent.append("SET ORIGINAL_FILE_NAME=").append(escapeForBatch(message.getOriginalFileName())).append("\n");
        batchContent.append("SET FILE_SIZE=").append(message.getFileSize()).append("\n");
        batchContent.append("SET CONTENT_TYPE=").append(escapeForBatch(message.getContentType())).append("\n");
        batchContent.append("SET STORAGE_PATH=").append(escapeForBatch(message.getStoragePath())).append("\n");
        batchContent.append("SET UPLOAD_TIMESTAMP=").append(message.getUploadTimestamp()).append("\n");
        batchContent.append("SET UPLOADER_ID=").append(escapeForBatch(message.getUploaderId())).append("\n");
        batchContent.append("\n");
        batchContent.append("echo Processing file: %ORIGINAL_FILE_NAME%\n");
        batchContent.append("echo File ID: %FILE_ID%\n");
        batchContent.append("echo File Size: %FILE_SIZE% bytes\n");
        batchContent.append("echo Storage Path: %STORAGE_PATH%\n");
        batchContent.append("echo.\n");
        batchContent.append("\n");
        batchContent.append("REM Import CSV data to database\n");
        batchContent.append("echo Importing data from %STORAGE_PATH% ...\n");
        batchContent.append("\n");
        batchContent.append("REM Exit with success\n");
        batchContent.append("exit /b 0\n");

        Files.writeString(batchFilePath, batchContent.toString());

        logger.info("Generated batch file: {}", batchFilePath);

        return batchFilePath;
    }

    private String escapeForBatch(String value) {
        if (value == null) {
            return "";
        }
        // Escape special characters for Windows batch files
        return value.replace("^", "^^")
                    .replace("&", "^&")
                    .replace("<", "^<")
                    .replace(">", "^>")
                    .replace("|", "^|")
                    .replace("%", "%%");
    }

    public void deleteBatchFile(Path batchFilePath) {
        if (config.isDeleteAfterExecution()) {
            try {
                Files.deleteIfExists(batchFilePath);
                logger.info("Deleted batch file: {}", batchFilePath);
            } catch (IOException e) {
                logger.warn("Failed to delete batch file: {}", batchFilePath, e);
            }
        }
    }
}
