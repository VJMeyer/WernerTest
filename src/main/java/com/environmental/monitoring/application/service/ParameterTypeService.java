package com.environmental.monitoring.application.service;

import com.environmental.monitoring.application.port.in.ParameterTypeUseCase;
import com.environmental.monitoring.application.port.out.ParameterTypeRepositoryPort;
import com.environmental.monitoring.domain.model.ParameterType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParameterTypeService implements ParameterTypeUseCase {

    private final ParameterTypeRepositoryPort repositoryPort;

    @Override
    public ParameterType createParameterType(ParameterType parameterType) {
        return repositoryPort.save(parameterType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParameterType> getParameterTypeById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterType> getAllParameterTypes() {
        return repositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterType> getActiveParameterTypes() {
        return repositoryPort.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParameterType> getParameterTypesByCategory(String category) {
        return repositoryPort.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParameterType> getParameterTypeByCode(String parameterCode) {
        return repositoryPort.findByParameterCode(parameterCode);
    }

    @Override
    public ParameterType updateParameterType(UUID id, ParameterType updatedType) {
        return repositoryPort.findById(id)
                .map(existingType -> {
                    existingType.setName(updatedType.getName());
                    existingType.setDescription(updatedType.getDescription());
                    existingType.setParameterCode(updatedType.getParameterCode());
                    existingType.setUnitOfMeasure(updatedType.getUnitOfMeasure());
                    existingType.setMinValidValue(updatedType.getMinValidValue());
                    existingType.setMaxValidValue(updatedType.getMaxValidValue());
                    existingType.setPrecisionDigits(updatedType.getPrecisionDigits());
                    existingType.setCategory(updatedType.getCategory());
                    existingType.setActive(updatedType.getActive());
                    return repositoryPort.save(existingType);
                })
                .orElseThrow(() -> new RuntimeException("ParameterType not found with id: " + id));
    }

    @Override
    public void deleteParameterType(UUID id) {
        repositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countParameterTypes() {
        return repositoryPort.count();
    }
}
