package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.MeasuringSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeasuringSiteJpaRepository extends JpaRepository<MeasuringSite, UUID> {

    List<MeasuringSite> findByActiveTrue();

    @Query("SELECT ms FROM MeasuringSite ms LEFT JOIN FETCH ms.stations WHERE ms.id = :id")
    Optional<MeasuringSite> findByIdWithStations(@Param("id") UUID id);

    List<MeasuringSite> findByNameContainingIgnoreCase(String name);
}
