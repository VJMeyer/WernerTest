package de.kisters.hmt.cloud.services.upload.service;

import de.kisters.hmt.cloud.services.upload.model.UploadStatus;
import de.kisters.hmt.cloud.services.upload.repository.UploadStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UploadStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UploadStatusService.class);

    private final UploadStatusRepository uploadStatusRepository;

    public UploadStatusService(UploadStatusRepository uploadStatusRepository) {
        this.uploadStatusRepository = uploadStatusRepository;
    }

    /**
     * Create a new upload status entry
     */
    public UploadStatus createUploadStatus(String filename, Long fileSize) {
        String uploadId = UUID.randomUUID().toString();
        UploadStatus status = new UploadStatus(uploadId, filename, fileSize);
        uploadStatusRepository.save(status);
        logger.info("Created upload status for upload ID: {}", uploadId);
        return status;
    }

    /**
     * Create a new upload status entry with a specific uploadId (for TUS protocol)
     */
    public UploadStatus createUploadStatus(String uploadId, String filename, Long fileSize) {
        UploadStatus status = new UploadStatus(uploadId, filename, fileSize);
        uploadStatusRepository.save(status);
        logger.info("Created upload status for upload ID: {}", uploadId);
        return status;
    }

    /**
     * Update upload progress
     */
    public void updateProgress(String uploadId, Long bytesUploaded) {
        Optional<UploadStatus> optionalStatus = uploadStatusRepository.findById(uploadId);
        if (optionalStatus.isPresent()) {
            UploadStatus status = optionalStatus.get();
            status.updateProgress(bytesUploaded);
            uploadStatusRepository.save(status);
            logger.debug("Updated progress for upload ID {}: {} bytes", uploadId, bytesUploaded);
        } else {
            logger.warn("Upload status not found for ID: {}", uploadId);
        }
    }

    /**
     * Mark upload as processing
     */
    public void markProcessing(String uploadId) {
        Optional<UploadStatus> optionalStatus = uploadStatusRepository.findById(uploadId);
        if (optionalStatus.isPresent()) {
            UploadStatus status = optionalStatus.get();
            status.markProcessing();
            uploadStatusRepository.save(status);
            logger.info("Marked upload {} as PROCESSING", uploadId);
        } else {
            logger.warn("Upload status not found for ID: {}", uploadId);
        }
    }

    /**
     * Mark upload as completed
     */
    public void markCompleted(String uploadId, String filePath, String checksum) {
        Optional<UploadStatus> optionalStatus = uploadStatusRepository.findById(uploadId);
        if (optionalStatus.isPresent()) {
            UploadStatus status = optionalStatus.get();
            status.markCompleted(filePath, checksum);
            uploadStatusRepository.save(status);
            logger.info("Marked upload {} as COMPLETED. TTL: 24 hours", uploadId);
        } else {
            logger.warn("Upload status not found for ID: {}", uploadId);
        }
    }

    /**
     * Mark upload as failed
     */
    public void markFailed(String uploadId, String errorMessage) {
        Optional<UploadStatus> optionalStatus = uploadStatusRepository.findById(uploadId);
        if (optionalStatus.isPresent()) {
            UploadStatus status = optionalStatus.get();
            status.markFailed(errorMessage);
            uploadStatusRepository.save(status);
            logger.error("Marked upload {} as FAILED: {}", uploadId, errorMessage);
        } else {
            logger.warn("Upload status not found for ID: {}", uploadId);
        }
    }

    /**
     * Get upload status by ID
     */
    public Optional<UploadStatus> getUploadStatus(String uploadId) {
        return uploadStatusRepository.findById(uploadId);
    }

    /**
     * Delete upload status (for manual cleanup if needed)
     */
    public void deleteUploadStatus(String uploadId) {
        uploadStatusRepository.deleteById(uploadId);
        logger.info("Deleted upload status for upload ID: {}", uploadId);
    }
}
