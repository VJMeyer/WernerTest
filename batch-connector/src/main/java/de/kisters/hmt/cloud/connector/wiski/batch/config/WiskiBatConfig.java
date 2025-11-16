package de.kisters.hmt.cloud.connector.wiski.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wiskibat")
public class WiskiBatConfig {

    private String executablePath = "wiskBat.exe";
    private String batchFileDirectory = "./batch-files";
    private int processTimeoutSeconds = 3600;
    private boolean deleteAfterExecution = false;

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public String getBatchFileDirectory() {
        return batchFileDirectory;
    }

    public void setBatchFileDirectory(String batchFileDirectory) {
        this.batchFileDirectory = batchFileDirectory;
    }

    public int getProcessTimeoutSeconds() {
        return processTimeoutSeconds;
    }

    public void setProcessTimeoutSeconds(int processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    public boolean isDeleteAfterExecution() {
        return deleteAfterExecution;
    }

    public void setDeleteAfterExecution(boolean deleteAfterExecution) {
        this.deleteAfterExecution = deleteAfterExecution;
    }
}
