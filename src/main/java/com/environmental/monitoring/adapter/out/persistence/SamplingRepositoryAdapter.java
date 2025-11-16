package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.SamplingJpaRepository;
import com.environmental.monitoring.application.port.out.SamplingRepositoryPort;
import com.environmental.monitoring.domain.model.Sampling;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SamplingRepositoryAdapter implements SamplingRepositoryPort {

    private final SamplingJpaRepository jpaRepository;

    @Override
    public Sampling save(Sampling sampling) {
        return jpaRepository.save(sampling);
    }

    @Override
    public Optional<Sampling> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Sampling> findByIdWithSamples(UUID id) {
        return jpaRepository.findByIdWithSamples(id);
    }

    @Override
    public List<Sampling> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Sampling> findByStationId(UUID stationId) {
        return jpaRepository.findByStationId(stationId);
    }

    @Override
    public List<Sampling> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByDateRange(start, end);
    }

    @Override
    public List<Sampling> findByStatus(String status) {
        return jpaRepository.findByStatusIgnoreCase(status);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
