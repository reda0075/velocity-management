package com.velocity.velocity_management.team.service;

import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.mapper.CollaboratorMapper;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.team.dto.request.CreateTeamRequest;
import com.velocity.velocity_management.team.dto.request.UpdateTeamMembersRequest;
import com.velocity.velocity_management.team.dto.request.UpdateTeamRequest;
import com.velocity.velocity_management.team.dto.response.TeamResponse;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.mapper.TeamMapper;
import com.velocity.velocity_management.team.repository.TeamRepository;
import com.velocity.velocity_management.teammonthlycalculation.entity.TeamVelocity;
import com.velocity.velocity_management.teammonthlycalculation.service.TeamVelocityService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorMapper collaboratorMapper;
    private final TeamVelocityService teamVelocityService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMapper teamMapper,
            CollaboratorRepository collaboratorRepository,
            CollaboratorMapper collaboratorMapper,
            @Lazy TeamVelocityService teamVelocityService) {

        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorMapper = collaboratorMapper;
        this.teamVelocityService = teamVelocityService;
    }

    public TeamResponse createTeam(CreateTeamRequest request) {

        if (teamRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "A team with this name already exists"
            );
        }

        Team team = teamMapper.toEntity(request);

        team = teamRepository.save(team);

        return teamMapper.toResponse(team);
    }

    public List<TeamResponse> getAllTeams() {

        return teamRepository.findAll()
                .stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    public TeamResponse getTeamById(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team with ID " + id + " not found"
                        ));

        return teamMapper.toResponse(team);
    }

    @Transactional(readOnly = true)
    public List<CollaboratorResponse> getTeamMembers(Long id) {

        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Team with ID " + id + " not found"
            );
        }

        return collaboratorRepository.findByTeamIdAndActiveTrue(id)
                .stream()
                .map(collaboratorMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CollaboratorResponse> updateTeamMembers(
            Long id, UpdateTeamMembersRequest request) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team with ID " + id + " not found"
                ));

        List<Collaborator> currentMembers =
                collaboratorRepository.findByTeamIdAndActiveTrue(id);

        List<Long> targetIds = request.getCollaboratorIds();

        List<Collaborator> targetCollaborators =
                collaboratorRepository.findAllById(targetIds);

        if (targetCollaborators.size() != targetIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more collaborator IDs do not exist"
            );
        }

        for (Collaborator current : currentMembers) {
            if (!targetIds.contains(current.getId())) {
                current.setTeam(null);
                collaboratorRepository.save(current);
            }
        }

        for (Collaborator target : targetCollaborators) {
            if (target.getTeam() == null || !target.getTeam().getId().equals(id)) {
                target.setTeam(team);
                collaboratorRepository.save(target);
            }
        }

        List<TeamVelocity> teamVelocities =
                teamVelocityService.findByTeamId(id);

        for (TeamVelocity tv : teamVelocities) {
            teamVelocityService.recalculateTeamVelocity(
                    tv.getTeam().getId(),
                    tv.getYear(),
                    tv.getMonth()
            );
        }

        return collaboratorRepository.findByTeamIdAndActiveTrue(id)
                .stream()
                .map(collaboratorMapper::toResponse)
                .toList();
    }

    public TeamResponse updateTeam(
            Long id,
            UpdateTeamRequest request) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team with ID " + id + " not found"
                        ));

        if (!team.getName().equals(request.getName())
                && teamRepository.existsByName(request.getName())) {

            throw new IllegalArgumentException(
                    "A team with this name already exists"
            );
        }

        teamMapper.updateEntity(team, request);

        team = teamRepository.save(team);

        return teamMapper.toResponse(team);
    }

    public TeamResponse activateTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team with ID " + id + " not found"
                        ));

        team.setActive(true);

        team = teamRepository.save(team);

        return teamMapper.toResponse(team);
    }

    public TeamResponse deactivateTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team with ID " + id + " not found"
                        ));

        team.setActive(false);

        team = teamRepository.save(team);

        return teamMapper.toResponse(team);
    }

    public void deleteTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team with ID " + id + " not found"
                        ));

        teamRepository.delete(team);
    }
}