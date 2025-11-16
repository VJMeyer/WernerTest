package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.MeasuringSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for MeasuringSite use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface MeasuringSiteUseCase {

    MeasuringSite createMeasuringSite(MeasuringSite measuringSite);

    Optional<MeasuringSite> getMeasuringSiteById(UUID id);

    Optional<MeasuringSite> getMeasuringSiteByIdWithStations(UUID id);

    List<MeasuringSite> getAllMeasuringSites();

    List<MeasuringSite> getActiveMeasuringSites();

    MeasuringSite updateMeasuringSite(UUID id, MeasuringSite measuringSite);

    void deleteMeasuringSite(UUID id);

    long countMeasuringSites();
}
