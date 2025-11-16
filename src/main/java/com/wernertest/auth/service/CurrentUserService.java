package com.wernertest.auth.service;

import com.wernertest.auth.dto.CurrentUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CurrentUserService {

    /**
     * Extracts current user information from the JWT token.
     */
    public CurrentUserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT token found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replace("ROLE_", ""))
            .collect(Collectors.toList());

        String organization = extractOrganization(jwt);

        return CurrentUserDTO.builder()
            .id(jwt.getSubject())
            .username(jwt.getClaimAsString("preferred_username"))
            .email(jwt.getClaimAsString("email"))
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .roles(roles)
            .organization(organization)
            .tokenExpiresAt(jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : null)
            .build();
    }

    /**
     * Gets the organization name from the JWT token.
     */
    public String getCurrentUserOrganization(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT token found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return extractOrganization(jwt);
    }

    /**
     * Gets the username from the JWT token.
     */
    public String getCurrentUsername(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No valid JWT token found");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("preferred_username");
    }

    /**
     * Checks if the current user has a specific role.
     */
    public boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }

    @SuppressWarnings("unchecked")
    private String extractOrganization(Jwt jwt) {
        // Try to get organization from custom claim
        Object orgClaim = jwt.getClaim("organization");
        if (orgClaim instanceof String) {
            return (String) orgClaim;
        }
        if (orgClaim instanceof List) {
            List<String> orgs = (List<String>) orgClaim;
            if (!orgs.isEmpty()) {
                return orgs.get(0);
            }
        }

        // Try to get from attributes
        Map<String, Object> attributes = jwt.getClaim("attributes");
        if (attributes != null && attributes.containsKey("organization")) {
            Object org = attributes.get("organization");
            if (org instanceof String) {
                return (String) org;
            }
            if (org instanceof List) {
                List<String> orgs = (List<String>) org;
                if (!orgs.isEmpty()) {
                    return orgs.get(0);
                }
            }
        }

        return "";
    }
}
