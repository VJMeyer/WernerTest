package com.environmental.monitoring.application.port.out;

import com.environmental.monitoring.domain.model.ParameterType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for ParameterType persistence operations.
 * This is a driven port in the hexagonal architecture.
 */
public interface ParameterTypeRepositoryPort {

    ParameterType save(ParameterType parameterType);

    Optional<ParameterType> findById(UUID id);

    List<ParameterType> findAll();

    List<ParameterType> findAllActive();

    List<ParameterType> findByCategory(String category);

    Optional<ParameterType> findByParameterCode(String parameterCode);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();
}
