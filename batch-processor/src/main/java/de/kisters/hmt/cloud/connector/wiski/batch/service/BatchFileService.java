package de.kisters.hmt.cloud.connector.wiski.batch.service;

import de.kisters.hmt.cloud.messaging.dto.FileUploadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BatchFileService {

    private static final Logger logger = LoggerFactory.getLogger(BatchFileService.class);

    @Value("${batch.directory}")
    private String batchDirectory;

    @Value("${wiskbat.executable.path}")
    private String wiskBatExecutablePath;

    @Value("${wiskbat.enabled:true}")
    private boolean wiskBatEnabled;

    public void processFileUpload(FileUploadMessage message) throws IOException, InterruptedException {
        logger.info("Processing file upload for: {}", message.getOriginalFilename());

        // Create batch directory if it doesn't exist
        Path batchDirPath = Paths.get(batchDirectory);
        if (!Files.exists(batchDirPath)) {
            Files.createDirectories(batchDirPath);
            logger.info("Created batch directory: {}", batchDirPath);
        }

        // Generate batch file
        String batchFileName = generateBatchFileName(message);
        Path batchFilePath = batchDirPath.resolve(batchFileName);

        createBatchFile(batchFilePath, message);
        logger.info("Created batch file: {}", batchFilePath);

        // Execute wiskBat if enabled
        if (wiskBatEnabled) {
            executeWiskBat(batchFilePath);
        } else {
            logger.info("WiskBat execution is disabled. Batch file created at: {}", batchFilePath);
        }
    }

    private String generateBatchFileName(FileUploadMessage message) {
        String timestamp = message.getUploadTimestamp()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseFilename = message.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9.-]", "_");
        return String.format("process_%s_%s.bat", timestamp, baseFilename);
    }

    private void createBatchFile(Path batchFilePath, FileUploadMessage message) throws IOException {
        List<String> batchCommands = new ArrayList<>();

        // Batch file header
        batchCommands.add("@echo off");
        batchCommands.add("REM Batch file generated for file upload processing");
        batchCommands.add("REM Generated at: " + message.getUploadTimestamp());
        batchCommands.add("");

        // File metadata as environment variables
        batchCommands.add("REM File Metadata");
        batchCommands.add("set ORIGINAL_FILENAME=" + message.getOriginalFilename());
        batchCommands.add("set STORED_FILENAME=" + message.getFilename());
        batchCommands.add("set FILE_PATH=" + message.getFilePath());
        batchCommands.add("set FILE_SIZE=" + message.getFileSize());
        batchCommands.add("set CONTENT_TYPE=" + message.getContentType());
        batchCommands.add("set CHECKSUM=" + message.getChecksum());
        batchCommands.add("set UPLOAD_TIMESTAMP=" + message.getUploadTimestamp());
        batchCommands.add("");

        // Echo metadata for logging
        batchCommands.add("echo Processing file: %ORIGINAL_FILENAME%");
        batchCommands.add("echo File size: %FILE_SIZE% bytes");
        batchCommands.add("echo File path: %FILE_PATH%");
        batchCommands.add("echo Checksum: %CHECKSUM%");
        batchCommands.add("");

        // Placeholder for wiskBat invocation
        batchCommands.add("REM WiskBat will process this file and extract CSV data to database");
        batchCommands.add("echo Calling wiskBat...");
        batchCommands.add("");

        // Write batch file
        try (BufferedWriter writer = Files.newBufferedWriter(batchFilePath)) {
            for (String command : batchCommands) {
                writer.write(command);
                writer.newLine();
            }
        }

        logger.debug("Batch file content written successfully");
    }

    private void executeWiskBat(Path batchFilePath) throws IOException, InterruptedException {
        logger.info("Executing wiskBat with batch file: {}", batchFilePath);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Check if running on Windows
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows: execute the batch file with wiskBat
                processBuilder.command(wiskBatExecutablePath, batchFilePath.toString());
            } else {
                // Linux/Unix: For demonstration, just execute the batch file as a shell script
                // In production, you might need Wine or a different approach
                logger.warn("Running on non-Windows platform. WiskBat execution may not work correctly.");
                processBuilder.command("bash", "-c", "cat " + batchFilePath.toString());
            }

            processBuilder.redirectErrorStream(true);

            logger.debug("Starting process: {}", String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            // Capture output
            try (var reader = process.inputReader()) {
                reader.lines().forEach(line -> logger.info("wiskBat output: {}", line));
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("wiskBat execution completed successfully");
            } else {
                logger.error("wiskBat execution failed with exit code: {}", exitCode);
                throw new RuntimeException("wiskBat execution failed with exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error executing wiskBat: {}", e.getMessage(), e);
            throw e;
        }
    }
}
