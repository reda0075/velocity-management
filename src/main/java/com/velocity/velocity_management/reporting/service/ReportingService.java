package com.velocity.velocity_management.reporting.service;

import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.entity.VelocityRitual;
import com.velocity.velocity_management.monthlycalculation.enums.VelocityStatus;
import com.velocity.velocity_management.monthlycalculation.repository.VelocityRepository;
import com.velocity.velocity_management.reporting.dto.response.CollaboratorReportResponse;
import com.velocity.velocity_management.reporting.dto.response.VelocityReportResponse;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingService {

    private final TeamRepository teamRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final VelocityRepository velocityRepository;

    public ReportingService(
            TeamRepository teamRepository,
            CollaboratorRepository collaboratorRepository,
            VelocityRepository velocityRepository) {

        this.teamRepository = teamRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.velocityRepository = velocityRepository;
    }

    @Transactional(readOnly = true)
    public VelocityReportResponse generateVelocityReport(
            Long teamId,
            Integer year,
            Integer month) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team ID " + teamId + " not found"
                        )
                );

        List<Collaborator> collaborators =
                collaboratorRepository.findByTeamIdAndActiveTrue(teamId);

        List<Velocity> velocities =
                velocityRepository
                        .findByCollaborator_Team_IdAndYearAndMonth(
                                teamId,
                                year,
                                month
                        );

        VelocityReportResponse response =
                new VelocityReportResponse();

        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setYear(year);
        response.setMonth(month);

        response.setTeamMembers(
                collaborators.size()
        );

        List<CollaboratorReportResponse> collaboratorReports =
                new ArrayList<>();

        double totalVelocity = 0;

        int velocityCount = 0;

        int pendingValidations = 0;

        int validatedVelocities = 0;

        for (Velocity velocity : velocities) {

            Collaborator collaborator =
                    velocity.getCollaborator();

            CollaboratorReportResponse collaboratorReport =
                    new CollaboratorReportResponse();

            collaboratorReport.setCollaboratorId(
                    collaborator.getId()
            );

            collaboratorReport.setMatricule(
                    collaborator.getMatricule()
            );

            collaboratorReport.setFirstName(
                    collaborator.getFirstName()
            );

            collaboratorReport.setLastName(
                    collaborator.getLastName()
            );

            collaboratorReport.setProfile(
                    collaborator.getProfile().name()
            );

            collaboratorReport.setVelocity(
                    velocity.getVelocity()
            );

            double totalRitualTimeHours = 0;

            for (VelocityRitual velocityRitual :
                    velocity.getRituals()) {

                double duration =
                        velocityRitual
                                .getRitual()
                                .getDurationHours();

                int occurrences =
                        velocityRitual.getOccurrences();

                totalRitualTimeHours +=
                        duration * occurrences;
            }

            double ritualTimeDays =
                    totalRitualTimeHours / 8;

            double effectiveWorkingDays =
                    velocity.getWorkingDays()
                            - ritualTimeDays;

            collaboratorReport.setRitualTimeHours(
                    totalRitualTimeHours
            );

            collaboratorReport.setRitualTimeDays(
                    ritualTimeDays
            );

            collaboratorReport.setEffectiveWorkingDays(
                    effectiveWorkingDays
            );

            VelocityStatus status =
                    velocity.getStatus();

            if (status != null) {

                collaboratorReport.setStatus(
                        status.name()
                );

                if (status == VelocityStatus.PENDING_VALIDATION) {

                    pendingValidations++;

                } else if (status == VelocityStatus.VALIDATED) {

                    validatedVelocities++;
                }
            }

            collaboratorReports.add(
                    collaboratorReport
            );

            totalVelocity += velocity.getVelocity();

            velocityCount++;
        }

        double teamVelocity =
                velocityCount > 0
                        ? totalVelocity / velocityCount
                        : 0;

        response.setCalculatedMembers(velocityCount);

        response.setTeamVelocity(teamVelocity);

        response.setAverageIndividualVelocity(teamVelocity);

        response.setPendingValidations(pendingValidations);

        response.setValidatedVelocities(validatedVelocities);

        response.setCollaborators(collaboratorReports);

        return response;
    }
}