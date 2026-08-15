package com.velocity.velocity_management.teammonthlycalculation.mapper;

import com.velocity.velocity_management.teammonthlycalculation.dto.response.CollaboratorVelocitySummary;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityDetailResponse;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityResponse;
import com.velocity.velocity_management.teammonthlycalculation.entity.TeamVelocity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamVelocityMapper {

    public TeamVelocityResponse toResponse(TeamVelocity teamVelocity) {

        TeamVelocityResponse response = new TeamVelocityResponse();

        response.setId(teamVelocity.getId());
        response.setTeamId(teamVelocity.getTeam().getId());
        response.setTeamName(teamVelocity.getTeam().getName());
        response.setYear(teamVelocity.getYear());
        response.setMonth(teamVelocity.getMonth());
        response.setTeamVelocityRatio(teamVelocity.getTeamVelocityRatio());
        response.setTotalMembers(teamVelocity.getTotalMembers());
        response.setValidatedMembers(teamVelocity.getValidatedMembers());
        response.setUnvalidatedMembers(teamVelocity.getUnvalidatedMembers());
        response.setCreatedAt(teamVelocity.getCreatedAt());
        response.setUpdatedAt(teamVelocity.getUpdatedAt());

        return response;
    }

    public TeamVelocityDetailResponse toDetailResponse(
            TeamVelocity teamVelocity,
            List<CollaboratorVelocitySummary> members) {

        TeamVelocityDetailResponse response = new TeamVelocityDetailResponse();

        response.setId(teamVelocity.getId());
        response.setTeamId(teamVelocity.getTeam().getId());
        response.setTeamName(teamVelocity.getTeam().getName());
        response.setYear(teamVelocity.getYear());
        response.setMonth(teamVelocity.getMonth());
        response.setTeamVelocityRatio(teamVelocity.getTeamVelocityRatio());
        response.setTotalMembers(teamVelocity.getTotalMembers());
        response.setValidatedMembers(teamVelocity.getValidatedMembers());
        response.setUnvalidatedMembers(teamVelocity.getUnvalidatedMembers());
        response.setCreatedAt(teamVelocity.getCreatedAt());
        response.setUpdatedAt(teamVelocity.getUpdatedAt());
        response.setMembers(members);

        return response;
    }
}
