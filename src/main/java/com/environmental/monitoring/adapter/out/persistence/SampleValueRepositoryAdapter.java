package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.SampleValueJpaRepository;
import com.environmental.monitoring.application.port.out.SampleValueRepositoryPort;
import com.environmental.monitoring.domain.model.SampleValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SampleValueRepositoryAdapter implements SampleValueRepositoryPort {

    private final SampleValueJpaRepository jpaRepository;

    @Override
    public SampleValue save(SampleValue sampleValue) {
        return jpaRepository.save(sampleValue);
    }

    @Override
    public Optional<SampleValue> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SampleValue> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<SampleValue> findBySampleId(UUID sampleId) {
        return jpaRepository.findBySampleId(sampleId);
    }

    @Override
    public List<SampleValue> findByParameterTypeId(UUID parameterTypeId) {
        return jpaRepository.findByParameterTypeId(parameterTypeId);
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
