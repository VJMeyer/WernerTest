package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.Sample;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for Sample use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface SampleUseCase {

    Sample createSample(Sample sample);

    Optional<Sample> getSampleById(UUID id);

    Optional<Sample> getSampleByIdWithValues(UUID id);

    List<Sample> getAllSamples();

    List<Sample> getSamplesBySamplingId(UUID samplingId);

    List<Sample> getSamplesByType(String sampleType);

    Sample updateSample(UUID id, Sample sample);

    void deleteSample(UUID id);

    long countSamples();
}
