package com.wernertest.auth.service;

import com.wernertest.auth.dto.OrganizationDTO;
import com.wernertest.auth.entity.Organization;
import com.wernertest.auth.exception.OrganizationNotFoundException;
import com.wernertest.auth.exception.UserManagementException;
import com.wernertest.auth.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String realm;

    @Transactional
    public OrganizationDTO createOrganization(OrganizationDTO request) {
        log.info("Creating organization: {}", request.getName());

        if (organizationRepository.existsByName(request.getName())) {
            throw new UserManagementException("Organization with name " + request.getName() + " already exists");
        }

        try {
            // Create group in Keycloak
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            GroupRepresentation group = new GroupRepresentation();
            group.setName(request.getName());

            Response response = realmResource.groups().add(group);
            if (response.getStatus() != 201) {
                throw new UserManagementException("Failed to create organization group in Keycloak");
            }

            String groupId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            response.close();

            // Create organization in database
            Organization organization = Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .keycloakGroupId(groupId)
                .active(true)
                .build();

            organization = organizationRepository.save(organization);

            // Create organization-specific admin role if it doesn't exist
            createOrganizationRoles(request.getName(), realmResource);

            return mapToDTO(organization);

        } catch (UserManagementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating organization", e);
            throw new UserManagementException("Failed to create organization: " + e.getMessage(), e);
        }
    }

    public List<OrganizationDTO> getAllOrganizations() {
        return organizationRepository.findAll().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    public OrganizationDTO getOrganizationByName(String name) {
        Organization org = organizationRepository.findByName(name)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + name));
        return mapToDTO(org);
    }

    @Transactional
    public OrganizationDTO updateOrganization(String name, OrganizationDTO request) {
        Organization org = organizationRepository.findByName(name)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + name));

        if (request.getDescription() != null) {
            org.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            org.setActive(request.getActive());
        }

        org = organizationRepository.save(org);
        return mapToDTO(org);
    }

    private void createOrganizationRoles(String orgName, RealmResource realmResource) {
        // Ensure base roles exist
        List<String> baseRoles = Arrays.asList("USER", "ORG_ADMIN", "ADMIN");

        for (String roleName : baseRoles) {
            try {
                realmResource.roles().get(roleName).toRepresentation();
            } catch (Exception e) {
                // Role doesn't exist, create it
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription("Base role: " + roleName);
                realmResource.roles().create(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private OrganizationDTO mapToDTO(Organization org) {
        return OrganizationDTO.builder()
            .id(org.getId())
            .name(org.getName())
            .description(org.getDescription())
            .keycloakGroupId(org.getKeycloakGroupId())
            .active(org.getActive())
            .createdAt(org.getCreatedAt().toString())
            .updatedAt(org.getUpdatedAt().toString())
            .build();
    }
}
