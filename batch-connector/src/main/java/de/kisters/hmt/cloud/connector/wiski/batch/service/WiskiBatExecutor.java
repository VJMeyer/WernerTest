package de.kisters.hmt.cloud.connector.wiski.batch.service;

import de.kisters.hmt.cloud.connector.wiski.batch.config.WiskiBatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class WiskiBatExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WiskiBatExecutor.class);

    private final WiskiBatConfig config;

    public WiskiBatExecutor(WiskiBatConfig config) {
        this.config = config;
    }

    public ExecutionResult executeWiskBat(Path batchFilePath) {
        logger.info("Executing wiskBat with batch file: {}", batchFilePath);

        ProcessBuilder processBuilder = new ProcessBuilder(
                config.getExecutablePath(),
                batchFilePath.toString()
        );

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("wiskBat output: {}", line);
                }
            }

            boolean completed = process.waitFor(config.getProcessTimeoutSeconds(), TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                logger.error("wiskBat execution timed out after {} seconds", config.getProcessTimeoutSeconds());
                return new ExecutionResult(false, -1, "Execution timed out", output.toString());
            }

            int exitCode = process.exitValue();
            boolean success = (exitCode == 0);

            if (success) {
                logger.info("wiskBat execution completed successfully for: {}", batchFilePath);
            } else {
                logger.error("wiskBat execution failed with exit code: {} for: {}", exitCode, batchFilePath);
            }

            return new ExecutionResult(success, exitCode,
                    success ? "Execution completed successfully" : "Execution failed with exit code: " + exitCode,
                    output.toString());

        } catch (IOException e) {
            logger.error("Failed to execute wiskBat: {}", e.getMessage(), e);
            return new ExecutionResult(false, -1, "Failed to execute: " + e.getMessage(), "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("wiskBat execution was interrupted", e);
            return new ExecutionResult(false, -1, "Execution was interrupted", "");
        }
    }

    public static class ExecutionResult {
        private final boolean success;
        private final int exitCode;
        private final String message;
        private final String output;

        public ExecutionResult(boolean success, int exitCode, String message, String output) {
            this.success = success;
            this.exitCode = exitCode;
            this.message = message;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getMessage() {
            return message;
        }

        public String getOutput() {
            return output;
        }

        @Override
        public String toString() {
            return "ExecutionResult{" +
                    "success=" + success +
                    ", exitCode=" + exitCode +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
