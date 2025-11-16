package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.MeasuringSiteUseCase;
import com.environmental.monitoring.application.port.out.MeasuringSiteRepositoryPort;
import com.environmental.monitoring.domain.model.MeasuringSite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MeasuringSiteService implements MeasuringSiteUseCase {

    private final MeasuringSiteRepositoryPort repositoryPort;

    @Override
    public MeasuringSite createMeasuringSite(MeasuringSite measuringSite) {
        return repositoryPort.save(measuringSite);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeasuringSite> getMeasuringSiteById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeasuringSite> getMeasuringSiteByIdWithStations(UUID id) {
        return repositoryPort.findByIdWithStations(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeasuringSite> getAllMeasuringSites() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeasuringSite> getActiveMeasuringSites() {
        return repositoryPort.findAllActive();
    }

    @Override
    public MeasuringSite updateMeasuringSite(UUID id, MeasuringSite updatedSite) {
        return repositoryPort.findById(id)
                .map(existingSite -> {
                    existingSite.setName(updatedSite.getName());
                    existingSite.setDescription(updatedSite.getDescription());
                    existingSite.setLatitude(updatedSite.getLatitude());
                    existingSite.setLongitude(updatedSite.getLongitude());
                    existingSite.setAddress(updatedSite.getAddress());
                    existingSite.setSiteCode(updatedSite.getSiteCode());
                    existingSite.setActive(updatedSite.getActive());
                    return repositoryPort.save(existingSite);
                })
                .orElseThrow(() -> new RuntimeException("MeasuringSite not found with id: " + id));
    }

    @Override
    public void deleteMeasuringSite(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMeasuringSites() {
        return repositoryPort.count();
    }
}
