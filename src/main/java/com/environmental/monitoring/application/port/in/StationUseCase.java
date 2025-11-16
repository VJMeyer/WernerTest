package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.Station;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for Station use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface StationUseCase {

    Station createStation(Station station);

    Optional<Station> getStationById(UUID id);

    Optional<Station> getStationByIdWithSamplings(UUID id);

    List<Station> getAllStations();

    List<Station> getActiveStations();

    List<Station> getStationsByMeasuringSiteId(UUID measuringSiteId);

    Station updateStation(UUID id, Station station);

    void deleteStation(UUID id);

    long countStations();
}
