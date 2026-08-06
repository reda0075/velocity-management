package com.velocity.velocity_management.collaborator.dto.request;

import com.velocity.velocity_management.collaborator.enums.Profile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateCollaboratorRequest {

    @NotBlank
    @Size(max = 30)
    private String firstName;

    @NotBlank
    @Size(max = 30)
    private String lastName;

    @NotNull
    private Profile profile;
    private boolean active;

}
