package com.velocity.velocity_management.collaborator.dto.response;

import com.velocity.velocity_management.collaborator.enums.Profile;

import java.time.LocalDateTime;

public class CollaboratorResponse {


    private Long id;
    private String matricule;
    private String firstName;
    private String lastName;
    private Profile profile;
    private boolean active ;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
