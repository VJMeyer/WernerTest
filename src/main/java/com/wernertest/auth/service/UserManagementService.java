package com.wernertest.auth.service;

import com.wernertest.auth.dto.*;
import com.wernertest.auth.exception.UserManagementException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final Keycloak keycloakAdminClient;
    private final AuditService auditService;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Creates a new user in Keycloak for the specified organization.
     * Only organization admins can create users for their organization.
     */
    public UserDTO createUser(CreateUserRequest request, String organizationName, String performedBy) {
        log.info("Creating user {} for organization {}", request.getUsername(), organizationName);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(request.getUsername(), true);
            if (!existingUsers.isEmpty()) {
                throw new UserManagementException("User with username " + request.getUsername() + " already exists");
            }

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Set organization attribute
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("organization", Collections.singletonList(organizationName));
            user.setAttributes(attributes);

            // Create the user
            Response response = usersResource.create(user);

            if (response.getStatus() != 201) {
                String error = response.readEntity(String.class);
                throw new UserManagementException("Failed to create user: " + error);
            }

            // Get created user ID from location header
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            response.close();

            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(request.getTemporaryPassword());
            usersResource.get(userId).resetPassword(credential);

            // Assign roles if specified
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                assignRolesToUser(userId, request.getRoles(), realmResource);
            } else {
                // Default role
                assignRolesToUser(userId, Collections.singletonList("USER"), realmResource);
            }

            // Add user to organization group
            addUserToOrganizationGroup(userId, organizationName, realmResource);

            // Audit log
            auditService.logAction("CREATE_USER", performedBy, request.getUsername(), organizationName,
                "Created user with roles: " + request.getRoles(), true, null);

            // Return created user
            return getUserById(userId);

        } catch (UserManagementException e) {
            auditService.logAction("CREATE_USER", performedBy, request.getUsername(), organizationName,
                "Failed to create user", false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating user", e);
            auditService.logAction("CREATE_USER", performedBy, request.getUsername(), organizationName,
                "Failed to create user", false, e.getMessage());
            throw new UserManagementException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all users belonging to a specific organization.
     */
    public List<UserDTO> getUsersByOrganization(String organizationName) {
        log.info("Fetching users for organization {}", organizationName);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Get all users and filter by organization attribute
            List<UserRepresentation> allUsers = usersResource.list();

            return allUsers.stream()
                .filter(user -> {
                    Map<String, List<String>> attrs = user.getAttributes();
                    if (attrs != null && attrs.containsKey("organization")) {
                        return attrs.get("organization").contains(organizationName);
                    }
                    return false;
                })
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching users for organization {}", organizationName, e);
            throw new UserManagementException("Failed to fetch users: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a user by their ID.
     */
    public UserDTO getUserById(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            return mapToUserDTO(user);
        } catch (Exception e) {
            log.error("Error fetching user {}", userId, e);
            throw new UserManagementException("Failed to fetch user: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing user.
     */
    public UserDTO updateUser(String userId, UpdateUserRequest request, String organizationName, String performedBy) {
        log.info("Updating user {} in organization {}", userId, organizationName);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Verify user belongs to the organization
            verifyUserOrganization(user, organizationName);

            // Update fields
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
            if (request.getEnabled() != null) {
                user.setEnabled(request.getEnabled());
            }

            userResource.update(user);

            // Update roles if specified
            if (request.getRoles() != null) {
                updateUserRoles(userId, request.getRoles(), realmResource);
            }

            auditService.logAction("UPDATE_USER", performedBy, user.getUsername(), organizationName,
                "Updated user details", true, null);

            return getUserById(userId);

        } catch (UserManagementException e) {
            auditService.logAction("UPDATE_USER", performedBy, userId, organizationName,
                "Failed to update user", false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating user {}", userId, e);
            auditService.logAction("UPDATE_USER", performedBy, userId, organizationName,
                "Failed to update user", false, e.getMessage());
            throw new UserManagementException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Resets a user's password.
     */
    public void resetUserPassword(String userId, ResetPasswordRequest request, String organizationName, String performedBy) {
        log.info("Resetting password for user {} in organization {}", userId, organizationName);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Verify user belongs to the organization
            verifyUserOrganization(user, organizationName);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(request.getTemporary());

            userResource.resetPassword(credential);

            auditService.logAction("RESET_PASSWORD", performedBy, user.getUsername(), organizationName,
                "Password reset" + (request.getTemporary() ? " (temporary)" : ""), true, null);

        } catch (UserManagementException e) {
            auditService.logAction("RESET_PASSWORD", performedBy, userId, organizationName,
                "Failed to reset password", false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error resetting password for user {}", userId, e);
            auditService.logAction("RESET_PASSWORD", performedBy, userId, organizationName,
                "Failed to reset password", false, e.getMessage());
            throw new UserManagementException("Failed to reset password: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a user from Keycloak.
     */
    public void deleteUser(String userId, String organizationName, String performedBy) {
        log.info("Deleting user {} from organization {}", userId, organizationName);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Verify user belongs to the organization
            verifyUserOrganization(user, organizationName);

            String username = user.getUsername();
            userResource.remove();

            auditService.logAction("DELETE_USER", performedBy, username, organizationName,
                "User deleted", true, null);

        } catch (UserManagementException e) {
            auditService.logAction("DELETE_USER", performedBy, userId, organizationName,
                "Failed to delete user", false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting user {}", userId, e);
            auditService.logAction("DELETE_USER", performedBy, userId, organizationName,
                "Failed to delete user", false, e.getMessage());
            throw new UserManagementException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Enables or disables a user.
     */
    public UserDTO setUserEnabled(String userId, boolean enabled, String organizationName, String performedBy) {
        UpdateUserRequest request = UpdateUserRequest.builder().enabled(enabled).build();
        return updateUser(userId, request, organizationName, performedBy);
    }

    private void assignRolesToUser(String userId, List<String> roleNames, RealmResource realmResource) {
        List<RoleRepresentation> roles = roleNames.stream()
            .map(roleName -> realmResource.roles().get(roleName).toRepresentation())
            .collect(Collectors.toList());

        realmResource.users().get(userId).roles().realmLevel().add(roles);
    }

    private void updateUserRoles(String userId, List<String> newRoleNames, RealmResource realmResource) {
        UserResource userResource = realmResource.users().get(userId);

        // Remove all current roles
        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        List<RoleRepresentation> rolesToRemove = currentRoles.stream()
            .filter(role -> !role.getName().startsWith("default-"))
            .filter(role -> !role.getName().equals("offline_access"))
            .filter(role -> !role.getName().equals("uma_authorization"))
            .collect(Collectors.toList());

        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }

        // Add new roles
        assignRolesToUser(userId, newRoleNames, realmResource);
    }

    private void addUserToOrganizationGroup(String userId, String organizationName, RealmResource realmResource) {
        try {
            List<GroupRepresentation> groups = realmResource.groups().groups(organizationName, 0, 1);
            if (!groups.isEmpty()) {
                realmResource.users().get(userId).joinGroup(groups.get(0).getId());
            }
        } catch (Exception e) {
            log.warn("Could not add user to organization group: {}", e.getMessage());
        }
    }

    private void verifyUserOrganization(UserRepresentation user, String expectedOrganization) {
        Map<String, List<String>> attrs = user.getAttributes();
        if (attrs == null || !attrs.containsKey("organization") ||
            !attrs.get("organization").contains(expectedOrganization)) {
            throw new UserManagementException("User does not belong to organization " + expectedOrganization);
        }
    }

    private UserDTO mapToUserDTO(UserRepresentation user) {
        List<String> roles = new ArrayList<>();
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            roles = realmResource.users().get(user.getId()).roles().realmLevel().listAll()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(name -> !name.startsWith("default-"))
                .filter(name -> !name.equals("offline_access"))
                .filter(name -> !name.equals("uma_authorization"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch roles for user {}", user.getId());
        }

        String organization = "";
        if (user.getAttributes() != null && user.getAttributes().containsKey("organization")) {
            organization = user.getAttributes().get("organization").get(0);
        }

        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.isEnabled())
            .roles(roles)
            .organization(organization)
            .createdTimestamp(user.getCreatedTimestamp())
            .build();
    }
}
