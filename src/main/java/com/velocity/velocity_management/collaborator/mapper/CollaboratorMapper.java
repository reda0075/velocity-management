package com.velocity.velocity_management.collaborator.mapper;


import com.velocity.velocity_management.collaborator.dto.request.CreateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.request.UpdateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import org.springframework.stereotype.Component;


@Component
public class CollaboratorMapper {

    public Collaborator toEntity(CreateCollaboratorRequest request){
        Collaborator collaborator = new Collaborator();
        collaborator.setFirstName(request.getFirstName());
        collaborator.setLastName(request.getLastName());
        collaborator.setProfile(request.getProfile());
        return collaborator;
    }


    public CollaboratorResponse toResponse(Collaborator collaborator) {

        CollaboratorResponse response = new CollaboratorResponse();

        response.setId(collaborator.getId());
        response.setMatricule(collaborator.getMatricule());
        response.setFirstName(collaborator.getFirstName());
        response.setLastName(collaborator.getLastName());
        response.setProfile(collaborator.getProfile());
        response.setTeamId( collaborator.getTeam() != null
                ? collaborator.getTeam().getId()
                : null
        );
        response.setTeamName(
                collaborator.getTeam() != null
                        ? collaborator.getTeam().getName()
                        : null
        );
        response.setActive(collaborator.isActive());
        response.setCreatedAt(collaborator.getCreatedAt());
        response.setUpdatedAt(collaborator.getUpdatedAt());

        return response;
    }
    public void updateEntity(
            Collaborator collaborator,
            UpdateCollaboratorRequest request) {

        collaborator.setFirstName(request.getFirstName());
        collaborator.setLastName(request.getLastName());
        collaborator.setProfile(request.getProfile());
    }

}
