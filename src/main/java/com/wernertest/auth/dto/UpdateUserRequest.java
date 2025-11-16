package com.wernertest.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String firstName;

    private String lastName;

    private Boolean enabled;

    private List<String> roles;
}
