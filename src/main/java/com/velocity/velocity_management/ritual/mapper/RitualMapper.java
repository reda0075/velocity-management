package com.velocity.velocity_management.ritual.mapper;

import com.velocity.velocity_management.collaborator.dto.request.CreateCollaboratorRequest;
import com.velocity.velocity_management.ritual.dto.request.CreateRitualRequest;
import com.velocity.velocity_management.ritual.dto.request.UpdateRitualRequest;
import com.velocity.velocity_management.ritual.dto.response.RitualResponse;
import com.velocity.velocity_management.ritual.entity.Ritual;
import org.springframework.stereotype.Component;

@Component
public class RitualMapper {

    public Ritual toEntity (CreateRitualRequest request){

        Ritual ritual = new Ritual();
        ritual.setName(request.getName());
        ritual.setDurationHours(request.getDurationHours());

        return ritual;
    }

    public RitualResponse toResponse (Ritual ritual){

        RitualResponse response = new RitualResponse();

        response.setId(ritual.getId());
        response.setName(ritual.getName());
        response.setDurationHours(ritual.getDurationHours());
        response.setCreatedAt(ritual.getUpdatedAt());
        response.setUpdatedAt(ritual.getCreatedAt());

        return response;
    }

    public void updateEntity(Ritual ritual, UpdateRitualRequest request){
        ritual.setName(request.getName());
        ritual.setDurationHours(request.getDurationHours());
    }


}
