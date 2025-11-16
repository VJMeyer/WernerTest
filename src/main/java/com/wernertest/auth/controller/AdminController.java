package com.wernertest.auth.controller;

import com.wernertest.auth.dto.ApiResponse;
import com.wernertest.auth.dto.OrganizationDTO;
import com.wernertest.auth.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "System administration endpoints (super admin only)")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrganizationService organizationService;

    @PostMapping("/organizations")
    @Operation(summary = "Create organization", description = "Create a new organization")
    public ResponseEntity<ApiResponse<OrganizationDTO>> createOrganization(
            @Valid @RequestBody OrganizationDTO request) {

        OrganizationDTO created = organizationService.createOrganization(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Organization created successfully", created));
    }

    @GetMapping("/organizations")
    @Operation(summary = "List organizations", description = "Get all organizations")
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> listOrganizations() {
        List<OrganizationDTO> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved", organizations));
    }

    @GetMapping("/organizations/{name}")
    @Operation(summary = "Get organization", description = "Get organization by name")
    public ResponseEntity<ApiResponse<OrganizationDTO>> getOrganization(@PathVariable String name) {
        OrganizationDTO organization = organizationService.getOrganizationByName(name);
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved", organization));
    }

    @PutMapping("/organizations/{name}")
    @Operation(summary = "Update organization", description = "Update organization details")
    public ResponseEntity<ApiResponse<OrganizationDTO>> updateOrganization(
            @PathVariable String name,
            @Valid @RequestBody OrganizationDTO request) {

        OrganizationDTO updated = organizationService.updateOrganization(name, request);

        return ResponseEntity.ok(ApiResponse.success("Organization updated", updated));
    }
}
