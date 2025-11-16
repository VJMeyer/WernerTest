package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.StationUseCase;
import com.environmental.monitoring.application.port.out.StationRepositoryPort;
import com.environmental.monitoring.domain.model.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StationService implements StationUseCase {

    private final StationRepositoryPort repositoryPort;

    @Override
    public Station createStation(Station station) {
        return repositoryPort.save(station);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Station> getStationById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Station> getStationByIdWithSamplings(UUID id) {
        return repositoryPort.findByIdWithSamplings(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Station> getAllStations() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Station> getActiveStations() {
        return repositoryPort.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Station> getStationsByMeasuringSiteId(UUID measuringSiteId) {
        return repositoryPort.findByMeasuringSiteId(measuringSiteId);
    }

    @Override
    public Station updateStation(UUID id, Station updatedStation) {
        return repositoryPort.findById(id)
                .map(existingStation -> {
                    existingStation.setName(updatedStation.getName());
                    existingStation.setDescription(updatedStation.getDescription());
                    existingStation.setStationCode(updatedStation.getStationCode());
                    existingStation.setStationType(updatedStation.getStationType());
                    existingStation.setLatitude(updatedStation.getLatitude());
                    existingStation.setLongitude(updatedStation.getLongitude());
                    existingStation.setElevationMeters(updatedStation.getElevationMeters());
                    existingStation.setActive(updatedStation.getActive());
                    existingStation.setInstallationDate(updatedStation.getInstallationDate());
                    return repositoryPort.save(existingStation);
                })
                .orElseThrow(() -> new RuntimeException("Station not found with id: " + id));
    }

    @Override
    public void deleteStation(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countStations() {
        return repositoryPort.count();
    }
}
