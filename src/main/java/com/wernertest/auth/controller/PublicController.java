package com.wernertest.auth.controller;

import com.wernertest.auth.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Public endpoints (no authentication required)")
public class PublicController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakIssuerUri;

    @GetMapping("/info")
    @Operation(summary = "Get application info", description = "Returns basic application information")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", applicationName);
        info.put("version", "1.0.0");
        info.put("authProvider", "Keycloak");
        info.put("authEndpoint", keycloakIssuerUri);

        return ResponseEntity.ok(ApiResponse.success("Application info retrieved", info));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns application health status")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "Application is running");

        return ResponseEntity.ok(ApiResponse.success("Health check passed", health));
    }
}
