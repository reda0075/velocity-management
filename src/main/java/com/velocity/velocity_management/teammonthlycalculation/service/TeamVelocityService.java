package com.velocity.velocity_management.teammonthlycalculation.service;

import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.enums.VelocityStatus;
import com.velocity.velocity_management.monthlycalculation.repository.VelocityRepository;
import com.velocity.velocity_management.monthlycalculation.service.VelocityService;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import com.velocity.velocity_management.teammonthlycalculation.dto.request.CreateTeamVelocityRequest;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.CollaboratorVelocitySummary;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityDetailResponse;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityResponse;
import com.velocity.velocity_management.teammonthlycalculation.entity.TeamVelocity;
import com.velocity.velocity_management.teammonthlycalculation.mapper.TeamVelocityMapper;
import com.velocity.velocity_management.teammonthlycalculation.repository.TeamVelocityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamVelocityService {

    private final TeamVelocityRepository teamVelocityRepository;
    private final TeamRepository teamRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final VelocityRepository velocityRepository;
    private final VelocityService velocityService;
    private final TeamVelocityMapper teamVelocityMapper;

    public TeamVelocityService(
            TeamVelocityRepository teamVelocityRepository,
            TeamRepository teamRepository,
            CollaboratorRepository collaboratorRepository,
            VelocityRepository velocityRepository,
            VelocityService velocityService,
            TeamVelocityMapper teamVelocityMapper) {

        this.teamVelocityRepository = teamVelocityRepository;
        this.teamRepository = teamRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.velocityRepository = velocityRepository;
        this.velocityService = velocityService;
        this.teamVelocityMapper = teamVelocityMapper;
    }

    @Transactional
    public TeamVelocityResponse createTeamVelocity(CreateTeamVelocityRequest request) {

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team ID " + request.getTeamId() + " not found"
                ));

        if (teamVelocityRepository.existsByTeamIdAndYearAndMonth(
                request.getTeamId(), request.getYear(), request.getMonth())) {

            throw new IllegalArgumentException(
                    "A team velocity calculation already exists for this team and month"
            );
        }

        List<Collaborator> members =
                collaboratorRepository.findByTeamIdAndActiveTrue(request.getTeamId());

        if (members.isEmpty()) {
            throw new IllegalArgumentException(
                    "This team has no active collaborators"
            );
        }

        List<Velocity> velocities = velocityRepository.findByCollaborator_Team_IdAndYearAndMonth(
                request.getTeamId(), request.getYear(), request.getMonth()
        );

        Map<Long, Velocity> velocityByCollaboratorId = velocities.stream()
                .collect(Collectors.toMap(
                        v -> v.getCollaborator().getId(),
                        Function.identity()
                ));

        List<Double> includedRatios = new ArrayList<>();
        int validatedCount = 0;

        for (Collaborator collaborator : members) {

            Velocity velocity = velocityByCollaboratorId.get(collaborator.getId());

            if (velocity != null && velocity.getStatus() == VelocityStatus.VALIDATED) {
                includedRatios.add(velocityService.calculateVelocityRatio(velocity));
                validatedCount++;
            }
        }

        if (includedRatios.isEmpty()) {
            throw new IllegalArgumentException(
                    "No validated velocity calculations found for this team in this period"
            );
        }

        double teamVelocityRatio = includedRatios.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        TeamVelocity teamVelocity = new TeamVelocity();

        teamVelocity.setTeam(team);
        teamVelocity.setYear(request.getYear());
        teamVelocity.setMonth(request.getMonth());
        teamVelocity.setTeamVelocityRatio(teamVelocityRatio);
        teamVelocity.setTotalMembers(members.size());
        teamVelocity.setValidatedMembers(validatedCount);
        teamVelocity.setUnvalidatedMembers(members.size() - validatedCount);

        LocalDateTime now = LocalDateTime.now();
        teamVelocity.setCreatedAt(now);
        teamVelocity.setUpdatedAt(now);

        teamVelocity = teamVelocityRepository.save(teamVelocity);

        return teamVelocityMapper.toResponse(teamVelocity);
    }

    @Transactional(readOnly = true)
    public List<TeamVelocityResponse> getAllTeamVelocities() {

        return teamVelocityRepository.findAll()
                .stream()
                .map(teamVelocityMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamVelocityResponse getTeamVelocityById(Long id) {

        TeamVelocity teamVelocity = findOrThrow(id);

        return teamVelocityMapper.toResponse(teamVelocity);
    }

    @Transactional(readOnly = true)
    public TeamVelocityDetailResponse getTeamVelocityDetails(Long id) {

        TeamVelocity teamVelocity = findOrThrow(id);

        List<CollaboratorVelocitySummary> members = buildMemberSummaries(
                teamVelocity.getTeam().getId(),
                teamVelocity.getYear(),
                teamVelocity.getMonth()
        );

        return teamVelocityMapper.toDetailResponse(teamVelocity, members);
    }

    @Transactional
    public void deleteTeamVelocity(Long id) {

        TeamVelocity teamVelocity = findOrThrow(id);

        teamVelocityRepository.delete(teamVelocity);
    }

    private TeamVelocity findOrThrow(Long id) {

        return teamVelocityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team velocity ID " + id + " not found"
                ));
    }

    private List<CollaboratorVelocitySummary> buildMemberSummaries(
            Long teamId, Integer year, Integer month) {

        List<Collaborator> members =
                collaboratorRepository.findByTeamIdAndActiveTrue(teamId);

        List<Velocity> velocities = velocityRepository.findByCollaborator_Team_IdAndYearAndMonth(
                teamId, year, month
        );

        Map<Long, Velocity> velocityByCollaboratorId = velocities.stream()
                .collect(Collectors.toMap(
                        v -> v.getCollaborator().getId(),
                        Function.identity()
                ));

        List<CollaboratorVelocitySummary> summaries = new ArrayList<>();

        for (Collaborator collaborator : members) {

            Velocity velocity = velocityByCollaboratorId.get(collaborator.getId());

            String collaboratorName =
                    collaborator.getFirstName() + " " + collaborator.getLastName();

            if (velocity == null) {

                summaries.add(new CollaboratorVelocitySummary(
                        collaborator.getId(),
                        collaboratorName,
                        null,
                        "NOT_CALCULATED",
                        false
                ));

            } else {

                double ratio = velocityService.calculateVelocityRatio(velocity);
                boolean validated = velocity.getStatus() == VelocityStatus.VALIDATED;

                summaries.add(new CollaboratorVelocitySummary(
                        collaborator.getId(),
                        collaboratorName,
                        ratio,
                        velocity.getStatus().name(),
                        validated
                ));
            }
        }

        return summaries;
    }
}
