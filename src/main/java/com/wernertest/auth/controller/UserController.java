package com.wernertest.auth.controller;

import com.wernertest.auth.dto.ApiResponse;
import com.wernertest.auth.dto.CurrentUserDTO;
import com.wernertest.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and information endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<CurrentUserDTO>> getCurrentUser(Authentication authentication) {
        CurrentUserDTO user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved", user));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Returns detailed profile information")
    public ResponseEntity<ApiResponse<CurrentUserDTO>> getUserProfile(Authentication authentication) {
        CurrentUserDTO user = currentUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", user));
    }
}
