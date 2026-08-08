package com.velocity.velocity_management.collaborator.service;

import com.velocity.velocity_management.collaborator.dto.request.CreateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.request.UpdateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.mapper.CollaboratorMapper;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollaboratorService {
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorMapper collaboratorMapper;

    public CollaboratorService(CollaboratorRepository collaboratorRepository, CollaboratorMapper collaboratorMapper) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorMapper = collaboratorMapper;
    }

            public CollaboratorResponse createCollaborator(CreateCollaboratorRequest request) {
                Collaborator collaborator = collaboratorMapper.toEntity(request);
                LocalDateTime now = LocalDateTime.now();


                collaborator.setMatricule(generateMatricule(collaborator));

                collaborator.setActive(true);

                collaborator.setCreatedAt(now);

                collaborator.setUpdatedAt(now);

                collaborator = collaboratorRepository.save(collaborator);

                return collaboratorMapper.toResponse(collaborator);
            }



            private String generateMatricule(Collaborator collaborator) {

            String profileCode = collaborator.getProfile().getCode();

            String baseMatricule =
                    collaborator.getFirstName().substring(0, 1).toUpperCase()
                            + "."
                            + collaborator.getLastName().toUpperCase()
                            + "_"
                            + profileCode;

            String matricule = baseMatricule;

            int counter = 1;

            while (collaboratorRepository.existsByMatricule(matricule)) {

                matricule = baseMatricule + String.format("%02d", counter);

                counter++;
            }

            return matricule;
        }



            public List<CollaboratorResponse> getAllCollaborators() {

                return collaboratorRepository.findAll()
                        .stream()
                        .map(collaboratorMapper::toResponse)
                        .toList();
            }


            public CollaboratorResponse getCollaboratorById(Long id) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Collaborator with ID " + id + " not found"));

                return collaboratorMapper.toResponse(collaborator);
            }


            public CollaboratorResponse updateCollaborator(Long id, UpdateCollaboratorRequest request) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collaborator with ID " + id + " not found"));

                collaboratorMapper.updateEntity(collaborator, request);

                collaborator.setUpdatedAt(LocalDateTime.now());

                collaborator = collaboratorRepository.save(collaborator);

                return collaboratorMapper.toResponse(collaborator);
            }



            public CollaboratorResponse activateCollaborator(Long id) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collaborator with ID " + id + " not found"));

                collaborator.setActive(true);

                collaborator.setUpdatedAt(LocalDateTime.now());

                collaborator = collaboratorRepository.save(collaborator);

                return collaboratorMapper.toResponse(collaborator);
            }



            public CollaboratorResponse deactivateCollaborator(Long id) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collaborator with ID " + id + " not found"));

                collaborator.setActive(false);

                collaborator.setUpdatedAt(LocalDateTime.now());

                collaborator = collaboratorRepository.save(collaborator);

                return collaboratorMapper.toResponse(collaborator);
            }

    public void deleteCollaborator(Long id) {

        Collaborator collaborator = collaboratorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collaborator with ID " + id + " not found"));

        collaboratorRepository.delete(collaborator);
    }


}
