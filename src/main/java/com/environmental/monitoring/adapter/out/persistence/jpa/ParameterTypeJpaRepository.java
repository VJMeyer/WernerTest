package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.ParameterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParameterTypeJpaRepository extends JpaRepository<ParameterType, UUID> {

    List<ParameterType> findByActiveTrue();

    List<ParameterType> findByCategoryIgnoreCase(String category);

    Optional<ParameterType> findByParameterCodeIgnoreCase(String parameterCode);
}
