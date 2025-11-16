package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.SampleValueUseCase;
import com.environmental.monitoring.application.port.out.SampleValueRepositoryPort;
import com.environmental.monitoring.domain.model.SampleValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SampleValueService implements SampleValueUseCase {

    private final SampleValueRepositoryPort repositoryPort;

    @Override
    public SampleValue createSampleValue(SampleValue sampleValue) {
        return repositoryPort.save(sampleValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleValue> getSampleValueById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleValue> getAllSampleValues() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleValue> getSampleValuesBySampleId(UUID sampleId) {
        return repositoryPort.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleValue> getSampleValuesByParameterTypeId(UUID parameterTypeId) {
        return repositoryPort.findByParameterTypeId(parameterTypeId);
    }

    @Override
    public SampleValue updateSampleValue(UUID id, SampleValue updatedValue) {
        return repositoryPort.findById(id)
                .map(existingValue -> {
                    existingValue.setNumericValue(updatedValue.getNumericValue());
                    existingValue.setTextValue(updatedValue.getTextValue());
                    existingValue.setQualityCode(updatedValue.getQualityCode());
                    existingValue.setDetectionLimit(updatedValue.getDetectionLimit());
                    existingValue.setBelowDetectionLimit(updatedValue.getBelowDetectionLimit());
                    existingValue.setNotes(updatedValue.getNotes());
                    existingValue.setMeasurementMethod(updatedValue.getMeasurementMethod());
                    existingValue.setUncertainty(updatedValue.getUncertainty());
                    return repositoryPort.save(existingValue);
                })
                .orElseThrow(() -> new RuntimeException("SampleValue not found with id: " + id));
    }

    @Override
    public void deleteSampleValue(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSampleValues() {
        return repositoryPort.count();
    }
}
