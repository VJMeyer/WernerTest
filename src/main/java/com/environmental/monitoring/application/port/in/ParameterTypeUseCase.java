package com.environmental.monitoring.application.port.in;

import com.environmental.monitoring.domain.model.ParameterType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for ParameterType use cases.
 * This is a driving port in the hexagonal architecture.
 */
public interface ParameterTypeUseCase {

    ParameterType createParameterType(ParameterType parameterType);

    Optional<ParameterType> getParameterTypeById(UUID id);

    List<ParameterType> getAllParameterTypes();

    List<ParameterType> getActiveParameterTypes();

    List<ParameterType> getParameterTypesByCategory(String category);

    Optional<ParameterType> getParameterTypeByCode(String parameterCode);

    ParameterType updateParameterType(UUID id, ParameterType parameterType);

    void deleteParameterType(UUID id);

    long countParameterTypes();
}
