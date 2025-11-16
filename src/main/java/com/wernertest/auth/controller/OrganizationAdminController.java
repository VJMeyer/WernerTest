package com.wernertest.auth.controller;

import com.wernertest.auth.dto.*;
import com.wernertest.auth.entity.AuditLog;
import com.wernertest.auth.service.AuditService;
import com.wernertest.auth.service.CurrentUserService;
import com.wernertest.auth.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-admin")
@RequiredArgsConstructor
@Tag(name = "Organization Admin", description = "User management endpoints for organization administrators")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ADMIN')")
public class OrganizationAdminController {

    private final UserManagementService userManagementService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @PostMapping("/users")
    @Operation(summary = "Create user", description = "Create a new user in the organization")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        UserDTO createdUser = userManagementService.createUser(request, organization, performedBy);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", createdUser));
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Get all users in the organization")
    public ResponseEntity<ApiResponse<List<UserDTO>>> listUsers(Authentication authentication) {
        String organization = currentUserService.getCurrentUserOrganization(authentication);

        List<UserDTO> users = userManagementService.getUsersByOrganization(organization);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user", description = "Get a specific user by ID")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable String userId) {
        UserDTO user = userManagementService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        UserDTO updatedUser = userManagementService.updateUser(userId, request, organization, performedBy);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "Reset password", description = "Reset a user's password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        userManagementService.resetUserPassword(userId, request, organization, performedBy);

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/users/{userId}/enable")
    @Operation(summary = "Enable user", description = "Enable a disabled user")
    public ResponseEntity<ApiResponse<UserDTO>> enableUser(
            @PathVariable String userId,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        UserDTO user = userManagementService.setUserEnabled(userId, true, organization, performedBy);

        return ResponseEntity.ok(ApiResponse.success("User enabled", user));
    }

    @PostMapping("/users/{userId}/disable")
    @Operation(summary = "Disable user", description = "Disable an active user")
    public ResponseEntity<ApiResponse<UserDTO>> disableUser(
            @PathVariable String userId,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        UserDTO user = userManagementService.setUserEnabled(userId, false, organization, performedBy);

        return ResponseEntity.ok(ApiResponse.success("User disabled", user));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Permanently delete a user")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable String userId,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);
        String performedBy = currentUserService.getCurrentUsername(authentication);

        userManagementService.deleteUser(userId, organization, performedBy);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs", description = "Get audit logs for the organization")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String organization = currentUserService.getCurrentUserOrganization(authentication);

        Page<AuditLog> logs = auditService.getAuditLogsForOrganization(
            organization, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", logs));
    }
}
