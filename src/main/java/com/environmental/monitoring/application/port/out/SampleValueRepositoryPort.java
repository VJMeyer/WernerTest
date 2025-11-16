package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.SampleValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for SampleValue persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface SampleValueRepositoryPort {

    SampleValue save(SampleValue sampleValue);

    Optional<SampleValue> findById(UUID id);

    List<SampleValue> findAll();

    List<SampleValue> findBySampleId(UUID sampleId);

    List<SampleValue> findByParameterTypeId(UUID parameterTypeId);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();
}
