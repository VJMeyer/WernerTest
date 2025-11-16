package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.StationJpaRepository;
import com.environmental.monitoring.application.port.out.StationRepositoryPort;
import com.environmental.monitoring.domain.model.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StationRepositoryAdapter implements StationRepositoryPort {

    private final StationJpaRepository jpaRepository;

    @Override
    public Station save(Station station) {
        return jpaRepository.save(station);
    }

    @Override
    public Optional<Station> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Station> findByIdWithSamplings(UUID id) {
        return jpaRepository.findByIdWithSamplings(id);
    }

    @Override
    public List<Station> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Station> findAllActive() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<Station> findByMeasuringSiteId(UUID measuringSiteId) {
        return jpaRepository.findByMeasuringSiteId(measuringSiteId);
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
