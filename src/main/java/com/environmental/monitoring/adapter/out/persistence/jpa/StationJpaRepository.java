package com.environmental.monitoring.adapter.out.persistence.jpa;

import com.environmental.monitoring.domain.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationJpaRepository extends JpaRepository<Station, UUID> {

    List<Station> findByActiveTrue();

    List<Station> findByMeasuringSiteId(UUID measuringSiteId);

    @Query("SELECT s FROM Station s LEFT JOIN FETCH s.samplings WHERE s.id = :id")
    Optional<Station> findByIdWithSamplings(@Param("id") UUID id);
}
