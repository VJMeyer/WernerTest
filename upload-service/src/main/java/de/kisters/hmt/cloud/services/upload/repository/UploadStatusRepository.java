package de.kisters.hmt.cloud.services.upload.repository;

import de.kisters.hmt.cloud.services.upload.model.UploadStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadStatusRepository extends CrudRepository<UploadStatus, String> {
}
