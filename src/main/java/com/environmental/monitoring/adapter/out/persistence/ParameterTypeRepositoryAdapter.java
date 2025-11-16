package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.adapter.out.persistence.jpa.ParameterTypeJpaRepository;
import com.environmental.monitoring.application.port.out.ParameterTypeRepositoryPort;
import com.environmental.monitoring.domain.model.ParameterType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParameterTypeRepositoryAdapter implements ParameterTypeRepositoryPort {

    private final ParameterTypeJpaRepository jpaRepository;

    @Override
    public ParameterType save(ParameterType parameterType) {
        return jpaRepository.save(parameterType);
    }

    @Override
    public Optional<ParameterType> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ParameterType> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ParameterType> findAllActive() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<ParameterType> findByCategory(String category) {
        return jpaRepository.findByCategoryIgnoreCase(category);
    }

    @Override
    public Optional<ParameterType> findByParameterCode(String parameterCode) {
        return jpaRepository.findByParameterCodeIgnoreCase(parameterCode);
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
