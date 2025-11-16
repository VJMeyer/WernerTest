package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.Sampling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SamplingJpaRepository extends JpaRepository<Sampling, UUID> {

    List<Sampling> findByStationId(UUID stationId);

    @Query("SELECT s FROM Sampling s LEFT JOIN FETCH s.samples WHERE s.id = :id")
    Optional<Sampling> findByIdWithSamples(@Param("id") UUID id);

    @Query("SELECT s FROM Sampling s WHERE s.samplingDate BETWEEN :start AND :end ORDER BY s.samplingDate DESC")
    List<Sampling> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Sampling> findByStatusIgnoreCase(String status);
}
