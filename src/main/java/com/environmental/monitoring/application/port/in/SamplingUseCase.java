package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.Sampling;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for Sampling use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface SamplingUseCase {

    Sampling createSampling(Sampling sampling);

    Optional<Sampling> getSamplingById(UUID id);

    Optional<Sampling> getSamplingByIdWithSamples(UUID id);

    List<Sampling> getAllSamplings();

    List<Sampling> getSamplingsByStationId(UUID stationId);

    List<Sampling> getSamplingsByDateRange(LocalDateTime start, LocalDateTime end);

    List<Sampling> getSamplingsByStatus(String status);

    Sampling updateSampling(UUID id, Sampling sampling);

    void deleteSampling(UUID id);

    long countSamplings();
}
