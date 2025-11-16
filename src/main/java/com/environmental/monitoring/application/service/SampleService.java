package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.SampleUseCase;
import com.environmental.monitoring.application.port.out.SampleRepositoryPort;
import com.environmental.monitoring.domain.model.Sample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SampleService implements SampleUseCase {

    private final SampleRepositoryPort repositoryPort;

    @Override
    public Sample createSample(Sample sample) {
        return repositoryPort.save(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sample> getSampleById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sample> getSampleByIdWithValues(UUID id) {
        return repositoryPort.findByIdWithValues(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getAllSamples() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesBySamplingId(UUID samplingId) {
        return repositoryPort.findBySamplingId(samplingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByType(String sampleType) {
        return repositoryPort.findBySampleType(sampleType);
    }

    @Override
    public Sample updateSample(UUID id, Sample updatedSample) {
        return repositoryPort.findById(id)
                .map(existingSample -> {
                    existingSample.setSampleCode(updatedSample.getSampleCode());
                    existingSample.setSampleType(updatedSample.getSampleType());
                    existingSample.setCollectionTime(updatedSample.getCollectionTime());
                    existingSample.setDepthMeters(updatedSample.getDepthMeters());
                    existingSample.setNotes(updatedSample.getNotes());
                    existingSample.setPreservationMethod(updatedSample.getPreservationMethod());
                    existingSample.setContainerType(updatedSample.getContainerType());
                    existingSample.setQualityFlag(updatedSample.getQualityFlag());
                    return repositoryPort.save(existingSample);
                })
                .orElseThrow(() -> new RuntimeException("Sample not found with id: " + id));
    }

    @Override
    public void deleteSample(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSamples() {
        return repositoryPort.count();
    }
}
