package com.velocity.velocity_management.collaborator.service;

import com.velocity.velocity_management.auth.entity.User;
import com.velocity.velocity_management.auth.enums.Role;
import com.velocity.velocity_management.auth.repository.UserRepository;
import com.velocity.velocity_management.auth.security.CredentialEncryptionService;
import com.velocity.velocity_management.collaborator.dto.request.CreateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.request.UpdateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.response.CollaboratorCredentialsResponse;
import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.collaborator.dto.response.InitialCredentials;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.mapper.CollaboratorMapper;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollaboratorService {
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorMapper collaboratorMapper;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialCredentialsGenerator initialCredentialsGenerator;
    private final CredentialEncryptionService credentialEncryptionService;

    public CollaboratorService(CollaboratorRepository collaboratorRepository, CollaboratorMapper collaboratorMapper, TeamRepository teamRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, InitialCredentialsGenerator initialCredentialsGenerator, CredentialEncryptionService credentialEncryptionService) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorMapper = collaboratorMapper;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialCredentialsGenerator = initialCredentialsGenerator;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Transactional
            public CollaboratorResponse createCollaborator(CreateCollaboratorRequest request) {
                Collaborator collaborator = collaboratorMapper.toEntity(request);
                LocalDateTime now = LocalDateTime.now();

                if (request.getTeamId() != null) {
                    Team team = teamRepository.findById(request.getTeamId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Team ID " + request.getTeamId() + " not found"
                                    ));
                    collaborator.setTeam(team);
                }

                collaborator.setMatricule(generateMatricule(collaborator));

                collaborator.setActive(true);

                collaborator.setCreatedAt(now);

                collaborator.setUpdatedAt(now);

                // The matricule doubles as the login: username = matricule,
                // password = matricule + "_" + 4 random digits, stored BCrypt-hashed.
                InitialCredentials credentials = initialCredentialsGenerator.generate(collaborator.getMatricule());

                User user = new User();
                user.setUsername(credentials.username());
                user.setPassword(passwordEncoder.encode(credentials.temporaryPassword()));
                user.setEncryptedInitialPassword(credentialEncryptionService.encrypt(
                        credentials.temporaryPassword(), credentials.username()));
                user.setRole(Role.COLLABORATOR);
                collaborator.setUser(userRepository.save(user));

                collaborator = collaboratorRepository.save(collaborator);

                CollaboratorResponse response = collaboratorMapper.toResponse(collaborator);
                response.setInitialCredentials(credentials);
                return response;
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


    @Transactional(readOnly = true)
            public List<CollaboratorResponse> getAllCollaborators() {

                return collaboratorRepository.findAll()
                        .stream()
                        .map(collaboratorMapper::toResponse)
                        .toList();
            }

    @Transactional(readOnly = true)
    public CollaboratorResponse getCollaboratorById(Long id) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Collaborator with ID " + id + " not found"));

return collaboratorMapper.toResponse(collaborator);
            }

    /**
     * Decrypts the initial password issued when the collaborator was created, so an
     * administrator can display it again. The password is not re-derived and never reset:
     * a collaborator created before this value was kept has nothing to return.
     */
    @Transactional(readOnly = true)
    public CollaboratorCredentialsResponse getInitialCredentials(Long id) {

        Collaborator collaborator = collaboratorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collaborator with ID " + id + " not found"));

        User user = collaborator.getUser();

        if (user == null || user.getEncryptedInitialPassword() == null) {
            throw new ResourceNotFoundException(
                    "No recoverable initial credentials for collaborator with ID " + id
                            + ". The password was never stored in a retrievable form and cannot be reconstructed.");
        }

        return new CollaboratorCredentialsResponse(
                user.getUsername(),
                credentialEncryptionService.decrypt(
                        user.getEncryptedInitialPassword(), user.getUsername()));
    }

    @Transactional(readOnly = true)
    public CollaboratorResponse getOwnProfile(String username) {
        Collaborator collaborator = collaboratorRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collaborator profile for user " + username + " not found"
                ));

        return collaboratorMapper.toResponse(collaborator);
    }

    @Transactional
            public CollaboratorResponse updateCollaborator(Long id, UpdateCollaboratorRequest request) {

                Collaborator collaborator = collaboratorRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collaborator with ID " + id + " not found"));


                if (request.getTeamId() != null) {
                    Team team = teamRepository.findById(request.getTeamId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Team ID " + request.getTeamId() + " not found"
                                    ));
                    collaborator.setTeam(team);
                } else {
                    collaborator.setTeam(null);
                }

                collaboratorMapper.updateEntity(collaborator, request);

                collaborator.setUpdatedAt(LocalDateTime.now());

                collaborator = collaboratorRepository.save(collaborator);

                return collaboratorMapper.toResponse(collaborator);
            }



    @Transactional
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



    @Transactional
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
