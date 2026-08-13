package com.velocity.velocity_management.collaborator.dto.response;

import com.velocity.velocity_management.collaborator.enums.Profile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorResponse {


    private Long id;
    private String matricule;
    private String firstName;
    private String lastName;
    private Profile profile;
    private Long teamId;
    private String teamName;
    private boolean active ;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
