package de.kisters.hmt.cloud.services.upload.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to extract user information from JWT token.
 *
 * Provides convenient access to:
 * - Username
 * - Email
 * - Roles
 * - Organization
 * - Organization ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserInfo {

    private String username;
    private String email;
    private List<String> roles;
    private String organization;
    private String orgId;

    /**
     * Extract user information from the current security context.
     *
     * @return JwtUserInfo with user details, or null if no authentication present
     */
    public static JwtUserInfo fromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            return new JwtUserInfo(
                    getUsername(jwt),
                    getEmail(jwt),
                    getRoles(jwtAuth),
                    getOrganization(jwt),
                    getOrgId(jwt)
            );
        }

        return null;
    }

    /**
     * Get username from JWT token.
     */
    private static String getUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null) {
            return preferredUsername;
        }
        return jwt.getClaimAsString("sub");
    }

    /**
     * Get email from JWT token.
     */
    private static String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * Get roles from authentication authorities.
     */
    private static List<String> getRoles(JwtAuthenticationToken authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (authorities == null) {
            return Collections.emptyList();
        }

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", "")) // Remove "ROLE_" prefix
                .collect(Collectors.toList());
    }

    /**
     * Get organization from JWT token.
     */
    private static String getOrganization(Jwt jwt) {
        // Try to get from custom claim
        Object orgClaim = jwt.getClaim("organization");
        if (orgClaim instanceof String) {
            return (String) orgClaim;
        } else if (orgClaim instanceof List) {
            List<?> orgList = (List<?>) orgClaim;
            if (!orgList.isEmpty()) {
                return String.valueOf(orgList.get(0));
            }
        }
        return null;
    }

    /**
     * Get organization ID from JWT token.
     */
    private static String getOrgId(Jwt jwt) {
        // Try to get from custom claim
        Object orgIdClaim = jwt.getClaim("orgId");
        if (orgIdClaim instanceof String) {
            return (String) orgIdClaim;
        } else if (orgIdClaim instanceof List) {
            List<?> orgIdList = (List<?>) orgIdClaim;
            if (!orgIdList.isEmpty()) {
                return String.valueOf(orgIdList.get(0));
            }
        }
        return null;
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user is an admin (ADMIN or ORG_ADMIN role).
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("ORG_ADMIN");
    }

    /**
     * Check if user belongs to a specific organization.
     */
    public boolean belongsToOrganization(String orgId) {
        return this.orgId != null && this.orgId.equals(orgId);
    }
}
