package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.SampleJpaRepository;
import com.environmental.monitoring.application.port.out.SampleRepositoryPort;
import com.environmental.monitoring.domain.model.Sample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SampleRepositoryAdapter implements SampleRepositoryPort {

    private final SampleJpaRepository jpaRepository;

    @Override
    public Sample save(Sample sample) {
        return jpaRepository.save(sample);
    }

    @Override
    public Optional<Sample> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Sample> findByIdWithValues(UUID id) {
        return jpaRepository.findByIdWithValues(id);
    }

    @Override
    public List<Sample> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Sample> findBySamplingId(UUID samplingId) {
        return jpaRepository.findBySamplingId(samplingId);
    }

    @Override
    public List<Sample> findBySampleType(String sampleType) {
        return jpaRepository.findBySampleTypeIgnoreCase(sampleType);
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
