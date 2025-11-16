package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.MeasuringSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for MeasuringSite persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface MeasuringSiteRepositoryPort {

    MeasuringSite save(MeasuringSite measuringSite);

    Optional<MeasuringSite> findById(UUID id);

    Optional<MeasuringSite> findByIdWithStations(UUID id);

    List<MeasuringSite> findAll();

    List<MeasuringSite> findAllActive();

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();

    List<MeasuringSite> findByNameContaining(String name);
}
