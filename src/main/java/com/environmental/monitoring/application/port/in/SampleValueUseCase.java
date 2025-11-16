package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.SampleValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for SampleValue use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface SampleValueUseCase {

    SampleValue createSampleValue(SampleValue sampleValue);

    Optional<SampleValue> getSampleValueById(UUID id);

    List<SampleValue> getAllSampleValues();

    List<SampleValue> getSampleValuesBySampleId(UUID sampleId);

    List<SampleValue> getSampleValuesByParameterTypeId(UUID parameterTypeId);

    SampleValue updateSampleValue(UUID id, SampleValue sampleValue);

    void deleteSampleValue(UUID id);

    long countSampleValues();
}
