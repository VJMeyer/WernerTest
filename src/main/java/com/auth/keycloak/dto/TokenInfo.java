package com.auth.keycloak.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TokenInfo {
    private String subject;
    private String issuer;
    private String audience;
    private Instant issuedAt;
    private Instant expiresAt;
    private String tokenType;
}
