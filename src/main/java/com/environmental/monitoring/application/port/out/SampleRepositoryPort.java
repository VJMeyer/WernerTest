package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.Sample;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Sample persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface SampleRepositoryPort {

    Sample save(Sample sample);

    Optional<Sample> findById(UUID id);

    Optional<Sample> findByIdWithValues(UUID id);

    List<Sample> findAll();

    List<Sample> findBySamplingId(UUID samplingId);

    List<Sample> findBySampleType(String sampleType);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();
}
