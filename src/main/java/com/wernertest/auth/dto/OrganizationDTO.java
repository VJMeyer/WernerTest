package com.wernertest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDTO {

    private Long id;

    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    private String keycloakGroupId;

    private Boolean active;

    private String createdAt;

    private String updatedAt;

    private Integer userCount;
}
