package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.Station;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Station persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface StationRepositoryPort {

    Station save(Station station);

    Optional<Station> findById(UUID id);

    Optional<Station> findByIdWithSamplings(UUID id);

    List<Station> findAll();

    List<Station> findAllActive();

    List<Station> findByMeasuringSiteId(UUID measuringSiteId);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();
}
