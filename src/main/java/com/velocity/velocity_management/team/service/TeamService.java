package com.velocity.velocity_management.team.service;

import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.team.dto.request.CreateTeamRequest;
import com.velocity.velocity_management.team.dto.request.UpdateTeamRequest;
import com.velocity.velocity_management.team.dto.response.TeamResponse;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.mapper.TeamMapper;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;

    public TeamService(
            TeamRepository teamRepository,
            TeamMapper teamMapper) {

        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
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