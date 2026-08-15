package com.velocity.velocity_management.collaborator.repository;

import com.velocity.velocity_management.collaborator.entity.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaboratorRepository extends JpaRepository<Collaborator , Long> {
    Optional<Collaborator> findByMatricule(String matricule);
    boolean existsByMatricule(String matricule);

    List<Collaborator> findByTeamIdAndActiveTrue(Long teamId);

}
