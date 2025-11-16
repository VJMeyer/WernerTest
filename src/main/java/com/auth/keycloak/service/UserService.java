package com.auth.keycloak.service;

import com.auth.keycloak.dto.TokenInfo;
import com.auth.keycloak.dto.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class UserService {

    public UserInfo getUserInfo(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            if (oauthToken.getPrincipal() instanceof OidcUser oidcUser) {
                return buildUserInfo(oidcUser);
            }
        }
        return UserInfo.builder()
            .username("Unknown")
            .email("N/A")
            .name("Unknown User")
            .realmRoles(Collections.emptyList())
            .clientRoles(Collections.emptyMap())
            .build();
    }

    public TokenInfo getTokenInfo(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            if (oauthToken.getPrincipal() instanceof OidcUser oidcUser) {
                return buildTokenInfo(oidcUser);
            }
        }
        return TokenInfo.builder()
            .subject("N/A")
            .issuer("N/A")
            .audience("N/A")
            .tokenType("Bearer")
            .build();
    }

    @SuppressWarnings("unchecked")
    private UserInfo buildUserInfo(OidcUser oidcUser) {
        Map<String, Object> claims = oidcUser.getClaims();

        List<String> realmRoles = new ArrayList<>();
        Map<String, List<String>> clientRoles = new HashMap<>();

        // Extract realm roles
        if (claims.containsKey("realm_access")) {
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess.containsKey("roles")) {
                realmRoles = new ArrayList<>((List<String>) realmAccess.get("roles"));
            }
        }

        // Extract client roles
        if (claims.containsKey("resource_access")) {
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            resourceAccess.forEach((clientId, access) -> {
                if (access instanceof Map) {
                    Map<String, Object> clientAccess = (Map<String, Object>) access;
                    if (clientAccess.containsKey("roles")) {
                        List<String> roles = new ArrayList<>((List<String>) clientAccess.get("roles"));
                        clientRoles.put(clientId, roles);
                    }
                }
            });
        }

        return UserInfo.builder()
            .subject(oidcUser.getSubject())
            .username(oidcUser.getPreferredUsername())
            .email(oidcUser.getEmail())
            .emailVerified(Boolean.TRUE.equals(oidcUser.getEmailVerified()))
            .name(oidcUser.getFullName())
            .givenName(oidcUser.getGivenName())
            .familyName(oidcUser.getFamilyName())
            .realmRoles(realmRoles)
            .clientRoles(clientRoles)
            .build();
    }

    private TokenInfo buildTokenInfo(OidcUser oidcUser) {
        String audience = "N/A";
        Object audClaim = oidcUser.getClaim("aud");
        if (audClaim instanceof List) {
            audience = String.join(", ", (List<String>) audClaim);
        } else if (audClaim instanceof String) {
            audience = (String) audClaim;
        } else if (oidcUser.getClaim("azp") != null) {
            audience = oidcUser.getClaim("azp");
        }

        Instant issuedAt = oidcUser.getIssuedAt();
        Instant expiresAt = oidcUser.getExpiresAt();

        return TokenInfo.builder()
            .subject(oidcUser.getSubject())
            .issuer(oidcUser.getIssuer() != null ? oidcUser.getIssuer().toString() : "N/A")
            .audience(audience)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .tokenType("Bearer")
            .build();
    }
}
