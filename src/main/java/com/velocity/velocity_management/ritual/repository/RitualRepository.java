package com.velocity.velocity_management.ritual.repository;

import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.ritual.entity.Ritual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RitualRepository extends JpaRepository<Ritual, Long> {

    Optional<Ritual> findByName(String name);
    boolean existsByName(String name);
}
