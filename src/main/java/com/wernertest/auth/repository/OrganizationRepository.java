package com.wernertest.auth.repository;

import com.wernertest.auth.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
    Optional<Organization> findByKeycloakGroupId(String keycloakGroupId);
    boolean existsByName(String name);
}
