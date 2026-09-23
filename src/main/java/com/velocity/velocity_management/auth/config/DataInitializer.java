package com.velocity.velocity_management.auth.config;

import com.velocity.velocity_management.auth.entity.User;
import com.velocity.velocity_management.auth.enums.Role;
import com.velocity.velocity_management.auth.repository.UserRepository;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.enums.Profile;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CollaboratorRepository collaboratorRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        User collaboratorUser = userRepository.findByUsername("collaborator").orElse(null);
        if (collaboratorUser == null) {
            collaboratorUser = new User();
            collaboratorUser.setUsername("collaborator");
            collaboratorUser.setPassword(passwordEncoder.encode("collaborator123"));
            collaboratorUser.setRole(Role.COLLABORATOR);
            collaboratorUser = userRepository.save(collaboratorUser);
        }

        if (collaboratorRepository.findByUserUsername("collaborator").isEmpty()) {
            Collaborator collaboratorProfile = new Collaborator();
            collaboratorProfile.setMatricule("T.COLLABORATOR_DEV");
            collaboratorProfile.setFirstName("Test");
            collaboratorProfile.setLastName("Collaborator");
            collaboratorProfile.setProfile(Profile.DEV);
            collaboratorProfile.setActive(true);
            collaboratorProfile.setCreatedAt(LocalDateTime.now());
            collaboratorProfile.setUpdatedAt(LocalDateTime.now());
            collaboratorProfile.setUser(collaboratorUser);
            collaboratorRepository.save(collaboratorProfile);
        }
    }
}
