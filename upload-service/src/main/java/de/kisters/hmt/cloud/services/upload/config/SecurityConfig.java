package de.kisters.hmt.cloud.services.upload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Security configuration for JWT authentication with Keycloak.
 *
 * Features:
 * - JWT token validation
 * - Role-based access control (RBAC)
 * - Organization-based user isolation
 * - Public health check endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Configure HTTP security with JWT authentication.
     *
     * Security rules:
     * - Login page and static resources are public
     * - Health endpoints are public
     * - Web pages and API endpoints require JWT authentication
     * - Stateless session management (JWT-based)
     * - CSRF disabled (JWT tokens used instead)
     *
     * Note: Web pages send JWT via Authorization header using HTMX interceptor (see layout.html)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF - using JWT tokens
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - login/logout pages and static resources
                .requestMatchers("/", "/login", "/logout", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico").permitAll()
                // Public API health check endpoints
                .requestMatchers("/api/upload/health", "/api/tus/health", "/actuator/**").permitAll()
                // All other endpoints (web pages and APIs) require JWT authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * JWT decoder bean for validating and decoding JWT tokens from Keycloak.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * Convert JWT claims to Spring Security authorities.
     *
     * Extracts:
     * - Realm roles (ADMIN, USER, ORG_ADMIN)
     * - Resource/client roles
     * - Custom claims (organization, orgId)
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * Custom converter to extract Keycloak roles from JWT token.
     *
     * Keycloak stores roles in a nested structure:
     * {
     *   "realm_access": { "roles": ["USER", "ADMIN"] },
     *   "resource_access": { "upload-service": { "roles": [...] } }
     * }
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm roles
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

            // Extract resource/client roles
            Collection<GrantedAuthority> resourceRoles = extractResourceRoles(jwt);

            // Combine all roles
            return Stream.concat(realmRoles.stream(), resourceRoles.stream())
                    .collect(Collectors.toSet());
        }

        /**
         * Extract roles from realm_access claim.
         */
        @SuppressWarnings("unchecked")
        private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || realmAccess.get("roles") == null) {
                return Collections.emptyList();
            }

            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        /**
         * Extract roles from resource_access claim.
         */
        @SuppressWarnings("unchecked")
        private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

            if (resourceAccess == null) {
                return Collections.emptyList();
            }

            return resourceAccess.values().stream()
                    .filter(resource -> resource instanceof Map)
                    .flatMap(resource -> {
                        Map<String, Object> resourceMap = (Map<String, Object>) resource;
                        if (resourceMap.get("roles") == null) {
                            return Stream.empty();
                        }
                        Collection<String> roles = (Collection<String>) resourceMap.get("roles");
                        return roles.stream();
                    })
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}
