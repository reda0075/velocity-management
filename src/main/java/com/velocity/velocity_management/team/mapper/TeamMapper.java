package com.velocity.velocity_management.team.mapper;

import com.velocity.velocity_management.team.dto.request.CreateTeamRequest;
import com.velocity.velocity_management.team.dto.request.UpdateTeamRequest;
import com.velocity.velocity_management.team.dto.response.TeamResponse;
import com.velocity.velocity_management.team.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public Team toEntity(CreateTeamRequest request) {

        Team team = new Team();

        team.setName(request.getName());
        team.setDescription(request.getDescription());

        return team;
    }

    public void updateEntity(
            Team team,
            UpdateTeamRequest request) {

        team.setName(request.getName());
        team.setDescription(request.getDescription());
    }

    public TeamResponse toResponse(Team team) {

        TeamResponse response = new TeamResponse();

        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setActive(team.isActive());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());

        return response;
    }
}