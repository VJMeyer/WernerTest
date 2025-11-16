package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.Sampling;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Sampling persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface SamplingRepositoryPort {

    Sampling save(Sampling sampling);

    Optional<Sampling> findById(UUID id);

    Optional<Sampling> findByIdWithSamples(UUID id);

    List<Sampling> findAll();

    List<Sampling> findByStationId(UUID stationId);

    List<Sampling> findByDateRange(LocalDateTime start, LocalDateTime end);

    List<Sampling> findByStatus(String status);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();
}
