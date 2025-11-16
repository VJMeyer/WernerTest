package de.kisters.hmt.cloud.services.upload.service;

import de.kisters.hmt.cloud.services.upload.config.StorageConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final StorageConfig storageConfig;
    private Path uploadPath;

    public FileStorageService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(storageConfig.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            logger.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public StoredFile storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String fileId = UUID.randomUUID().toString();
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String storedFileName = fileId + (extension.isEmpty() ? "" : "." + extension);

        Path targetPath = uploadPath.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new SecurityException("Cannot store file outside upload directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Stored file: {} as {} (size: {} bytes)", originalFileName, storedFileName, file.getSize());

        return new StoredFile(fileId, storedFileName, originalFileName,
                              file.getSize(), file.getContentType(), targetPath.toString());
    }

    public StoredFile storeFileStreaming(InputStream inputStream, String originalFileName,
                                          String contentType, long fileSize) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFileName);
        String storedFileName = fileId + (extension.isEmpty() ? "" : "." + extension);

        Path targetPath = uploadPath.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new SecurityException("Cannot store file outside upload directory");
        }

        long bytesWritten = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Stored large file: {} as {} (size: {} bytes)", originalFileName, storedFileName, bytesWritten);

        return new StoredFile(fileId, storedFileName, originalFileName,
                              bytesWritten, contentType, targetPath.toString());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static class StoredFile {
        private final String fileId;
        private final String storedFileName;
        private final String originalFileName;
        private final long fileSize;
        private final String contentType;
        private final String storagePath;

        public StoredFile(String fileId, String storedFileName, String originalFileName,
                          long fileSize, String contentType, String storagePath) {
            this.fileId = fileId;
            this.storedFileName = storedFileName;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.storagePath = storagePath;
        }

        public String getFileId() {
            return fileId;
        }

        public String getStoredFileName() {
            return storedFileName;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public String getContentType() {
            return contentType;
        }

        public String getStoragePath() {
            return storagePath;
        }
    }
}
