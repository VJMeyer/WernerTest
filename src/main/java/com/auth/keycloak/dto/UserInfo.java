package com.auth.keycloak.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserInfo {
    private String subject;
    private String username;
    private String email;
    private boolean emailVerified;
    private String name;
    private String givenName;
    private String familyName;
    private List<String> realmRoles;
    private Map<String, List<String>> clientRoles;
}
