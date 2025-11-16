package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.MeasuringSiteJpaRepository;
import com.environmental.monitoring.application.port.out.MeasuringSiteRepositoryPort;
import com.environmental.monitoring.domain.model.MeasuringSite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MeasuringSiteRepositoryAdapter implements MeasuringSiteRepositoryPort {

    private final MeasuringSiteJpaRepository jpaRepository;

    @Override
    public MeasuringSite save(MeasuringSite measuringSite) {
        return jpaRepository.save(measuringSite);
    }

    @Override
    public Optional<MeasuringSite> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<MeasuringSite> findByIdWithStations(UUID id) {
        return jpaRepository.findByIdWithStations(id);
    }

    @Override
    public List<MeasuringSite> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<MeasuringSite> findAllActive() {
        return jpaRepository.findByActiveTrue();
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

    @Override
    public List<MeasuringSite> findByNameContaining(String name) {
        return jpaRepository.findByNameContainingIgnoreCase(name);
    }
}
