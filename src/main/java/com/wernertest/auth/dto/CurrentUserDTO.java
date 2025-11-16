package com.wernertest.auth.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserDTO {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String organization;
    private Long tokenExpiresAt;
}
