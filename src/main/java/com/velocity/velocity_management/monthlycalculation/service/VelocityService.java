package com.velocity.velocity_management.monthlycalculation.service;

import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.monthlycalculation.enums.VelocityStatus;
import com.velocity.velocity_management.monthlycalculation.dto.request.CreateVelocityRequest;
import com.velocity.velocity_management.monthlycalculation.dto.request.VelocityRitualRequest;
import com.velocity.velocity_management.monthlycalculation.dto.response.VelocityResponse;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.entity.VelocityRitual;
import com.velocity.velocity_management.monthlycalculation.mapper.VelocityMapper;
import com.velocity.velocity_management.monthlycalculation.repository.VelocityRepository;
import com.velocity.velocity_management.ritual.entity.Ritual;
import com.velocity.velocity_management.ritual.repository.RitualRepository;
import com.velocity.velocity_management.teammonthlycalculation.service.TeamVelocityService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VelocityService {

    private final VelocityRepository velocityRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final RitualRepository ritualRepository;
    private final VelocityMapper velocityMapper;
    private final TeamVelocityService teamVelocityService;

    public VelocityService(
            VelocityRepository velocityRepository,
            CollaboratorRepository collaboratorRepository,
            RitualRepository ritualRepository,
            VelocityMapper velocityMapper,
            @Lazy TeamVelocityService teamVelocityService) {

        this.velocityRepository = velocityRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.ritualRepository = ritualRepository;
        this.velocityMapper = velocityMapper;
        this.teamVelocityService = teamVelocityService;
    }

    public VelocityResponse createVelocity(CreateVelocityRequest request) {

        if (velocityRepository.existsByCollaboratorIdAndYearAndMonth(
                request.getCollaboratorId(),
                request.getYear(),
                request.getMonth())) {

            throw new IllegalArgumentException(
                    "A velocity calculation already exists for this collaborator and month"
            );
        }

        Collaborator collaborator = collaboratorRepository.findById(
                request.getCollaboratorId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Collaborator ID " + request.getCollaboratorId() + " not found"
                )
        );

        Velocity velocity = new Velocity();

        velocity.setCollaborator(collaborator);
        velocity.setYear(request.getYear());
        velocity.setMonth(request.getMonth());
        velocity.setWorkingDays(request.getWorkingDays());
        velocity.setVelocity(request.getVelocity());

        velocity.setStatus(VelocityStatus.PENDING_VALIDATION);

        LocalDateTime now = LocalDateTime.now();

        velocity.setCreatedAt(now);
        velocity.setUpdatedAt(now);

        List<VelocityRitual> velocityRituals = new ArrayList<>();

        for (VelocityRitualRequest ritualRequest : request.getRituals()) {

            Ritual ritual = ritualRepository.findById(
                    ritualRequest.getRitualId()
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ritual ID " + ritualRequest.getRitualId() + " not found"
                    )
            );

            VelocityRitual velocityRitual = new VelocityRitual();

            velocityRitual.setVelocity(velocity);
            velocityRitual.setRitual(ritual);
            velocityRitual.setOccurrences(ritualRequest.getOccurrences());

            velocityRituals.add(velocityRitual);
        }

        velocity.setRituals(velocityRituals);

        velocity = velocityRepository.save(velocity);

        return buildResponse(velocity);
    }

    private VelocityResponse buildResponse(Velocity velocity) {

        double totalRitualTimeHours = totalRitualTimeHours(velocity);
        double ritualTimeDays = totalRitualTimeHours / 8;
        double effectiveWorkingDays = velocity.getWorkingDays() - ritualTimeDays;
        double velocityRatio = velocity.getVelocity() / effectiveWorkingDays;

        return velocityMapper.toResponse(
                velocity,
                totalRitualTimeHours,
                ritualTimeDays,
                effectiveWorkingDays,
                velocityRatio
        );
    }

    private double totalRitualTimeHours(Velocity velocity) {

        double totalRitualTimeHours = 0;

        for (VelocityRitual velocityRitual : velocity.getRituals()) {

            double duration = velocityRitual.getRitual().getDurationHours();

            int occurrences = velocityRitual.getOccurrences();

            totalRitualTimeHours += duration * occurrences;
        }

        return totalRitualTimeHours;
    }

    /**
     * Public helper so other modules (e.g. team velocity aggregation)
     * reuse the exact same ratio formula instead of duplicating it.
     */
    public double calculateVelocityRatio(Velocity velocity) {

        double totalRitualTimeHours = totalRitualTimeHours(velocity);
        double ritualTimeDays = totalRitualTimeHours / 8;
        double effectiveWorkingDays = velocity.getWorkingDays() - ritualTimeDays;

        return velocity.getVelocity() / effectiveWorkingDays;
    }

    public VelocityResponse validateVelocity(Long id) {

        Velocity velocity = velocityRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Velocity ID " + id + " not found"
                        ));

        velocity.setStatus(VelocityStatus.VALIDATED);
        velocity.setUpdatedAt(LocalDateTime.now());

        velocity = velocityRepository.save(velocity);

        Long collaboratorId = velocity.getCollaborator().getId();
        Collaborator collaborator = collaboratorRepository.findByIdWithTeam(collaboratorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collaborator ID " + collaboratorId + " not found"
                        )
                );

        if (collaborator.getTeam() != null) {
            teamVelocityService.recalculateTeamVelocity(
                    collaborator.getTeam().getId(),
                    velocity.getYear(),
                    velocity.getMonth()
            );
        }

        return buildResponse(velocity);
    }


    public List<VelocityResponse> getAllVelocities() {

        return velocityRepository.findAll()
                .stream()
                .map(this::buildResponse)
                .toList();
    }


    public VelocityResponse getVelocityById(Long id) {

        Velocity velocity = velocityRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Velocity ID " + id + " not found"
                        ));

        return buildResponse(velocity);
    }


    public void deleteVelocity(Long id) {

        Velocity velocity = velocityRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Velocity ID " + id + " not found"
                        ));

        velocityRepository.delete(velocity);
    }

    public VelocityResponse updateVelocity(
            Long id,
            CreateVelocityRequest request) {

        Velocity velocity = velocityRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Velocity ID " + id + " not found"
                        ));

        boolean duplicateExists =
                velocityRepository.existsByCollaboratorIdAndYearAndMonthAndIdNot(
                        request.getCollaboratorId(),
                        request.getYear(),
                        request.getMonth(),
                        id
                );

        if (duplicateExists) {
            throw new IllegalArgumentException(
                    "A velocity calculation already exists for this collaborator and month"
            );
        }

        Collaborator collaborator = collaboratorRepository.findById(
                request.getCollaboratorId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Collaborator ID " + request.getCollaboratorId() + " not found"
                )
        );

        velocity.setCollaborator(collaborator);
        velocity.setYear(request.getYear());
        velocity.setMonth(request.getMonth());
        velocity.setWorkingDays(request.getWorkingDays());

        velocity.setStatus(VelocityStatus.PENDING_VALIDATION);

        velocity.setVelocity(request.getVelocity());
        velocity.setUpdatedAt(LocalDateTime.now());

        velocity.getRituals().clear();

        for (VelocityRitualRequest ritualRequest : request.getRituals()) {

            Ritual ritual = ritualRepository.findById(
                    ritualRequest.getRitualId()
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Ritual ID " + ritualRequest.getRitualId() + " not found"
                    )
            );

            VelocityRitual velocityRitual = new VelocityRitual();

            velocityRitual.setVelocity(velocity);
            velocityRitual.setRitual(ritual);
            velocityRitual.setOccurrences(ritualRequest.getOccurrences());

            velocity.getRituals().add(velocityRitual);
        }

        velocity = velocityRepository.save(velocity);

        Long collaboratorId = velocity.getCollaborator().getId();
        Collaborator collaboratorWithTeam = collaboratorRepository.findByIdWithTeam(collaboratorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collaborator ID " + collaboratorId + " not found"
                        )
                );

        if (collaboratorWithTeam.getTeam() != null) {
            teamVelocityService.recalculateTeamVelocity(
                    collaboratorWithTeam.getTeam().getId(),
                    velocity.getYear(),
                    velocity.getMonth()
            );
        }

        return buildResponse(velocity);
    }
}