package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SampleJpaRepository extends JpaRepository<Sample, UUID> {

    List<Sample> findBySamplingId(UUID samplingId);

    @Query("SELECT s FROM Sample s LEFT JOIN FETCH s.sampleValues WHERE s.id = :id")
    Optional<Sample> findByIdWithValues(@Param("id") UUID id);

    List<Sample> findBySampleTypeIgnoreCase(String sampleType);
}
