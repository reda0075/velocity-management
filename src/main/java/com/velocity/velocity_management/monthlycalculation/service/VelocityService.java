package com.velocity.velocity_management.monthlycalculation.service;

import com.velocity.velocity_management.collaborator.entity.Collaborator;

import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.monthlycalculation.dto.request.CreateVelocityRequest;
import com.velocity.velocity_management.monthlycalculation.dto.request.VelocityRitualRequest;
import com.velocity.velocity_management.monthlycalculation.dto.response.VelocityResponse;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.entity.VelocityRitual;
import com.velocity.velocity_management.monthlycalculation.mapper.VelocityMapper;
import com.velocity.velocity_management.monthlycalculation.repository.VelocityRepository;
import com.velocity.velocity_management.ritual.entity.Ritual;
import com.velocity.velocity_management.ritual.repository.RitualRepository;
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

    public VelocityService(
            VelocityRepository velocityRepository,
            CollaboratorRepository collaboratorRepository,
            RitualRepository ritualRepository,
            VelocityMapper velocityMapper) {

        this.velocityRepository = velocityRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.ritualRepository = ritualRepository;
        this.velocityMapper = velocityMapper;
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

        double totalRitualTimeHours = 0;

        for (VelocityRitual velocityRitual : velocity.getRituals()) {

            double duration = velocityRitual.getRitual().getDurationHours();

            int occurrences = velocityRitual.getOccurrences();

            totalRitualTimeHours += duration * occurrences;
        }

        double ritualTimeDays = totalRitualTimeHours / 8;

        double effectiveWorkingDays =
                velocity.getWorkingDays() - ritualTimeDays;

        double velocityRatio =
                velocity.getVelocity() / effectiveWorkingDays;

        return velocityMapper.toResponse(
                velocity,
                totalRitualTimeHours,
                ritualTimeDays,
                effectiveWorkingDays,
                velocityRatio
        );
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

        return buildResponse(velocity);
    }
}