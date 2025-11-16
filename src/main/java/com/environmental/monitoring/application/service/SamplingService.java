package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.SamplingUseCase;
import com.environmental.monitoring.application.port.out.SamplingRepositoryPort;
import com.environmental.monitoring.domain.model.Sampling;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SamplingService implements SamplingUseCase {

    private final SamplingRepositoryPort repositoryPort;

    @Override
    public Sampling createSampling(Sampling sampling) {
        return repositoryPort.save(sampling);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sampling> getSamplingById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sampling> getSamplingByIdWithSamples(UUID id) {
        return repositoryPort.findByIdWithSamples(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sampling> getAllSamplings() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sampling> getSamplingsByStationId(UUID stationId) {
        return repositoryPort.findByStationId(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sampling> getSamplingsByDateRange(LocalDateTime start, LocalDateTime end) {
        return repositoryPort.findByDateRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sampling> getSamplingsByStatus(String status) {
        return repositoryPort.findByStatus(status);
    }

    @Override
    public Sampling updateSampling(UUID id, Sampling updatedSampling) {
        return repositoryPort.findById(id)
                .map(existingSampling -> {
                    existingSampling.setSamplingCode(updatedSampling.getSamplingCode());
                    existingSampling.setSamplingDate(updatedSampling.getSamplingDate());
                    existingSampling.setSamplerName(updatedSampling.getSamplerName());
                    existingSampling.setNotes(updatedSampling.getNotes());
                    existingSampling.setWeatherConditions(updatedSampling.getWeatherConditions());
                    existingSampling.setAirTemperature(updatedSampling.getAirTemperature());
                    existingSampling.setStatus(updatedSampling.getStatus());
                    return repositoryPort.save(existingSampling);
                })
                .orElseThrow(() -> new RuntimeException("Sampling not found with id: " + id));
    }

    @Override
    public void deleteSampling(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSamplings() {
        return repositoryPort.count();
    }
}
