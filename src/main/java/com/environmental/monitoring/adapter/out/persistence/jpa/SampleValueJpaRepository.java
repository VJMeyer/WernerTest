package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.SampleValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SampleValueJpaRepository extends JpaRepository<SampleValue, UUID> {

    List<SampleValue> findBySampleId(UUID sampleId);

    List<SampleValue> findByParameterTypeId(UUID parameterTypeId);
}
