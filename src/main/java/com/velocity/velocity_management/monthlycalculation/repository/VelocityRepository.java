package com.velocity.velocity_management.monthlycalculation.repository;

import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VelocityRepository extends JpaRepository<Velocity, Long> {

    boolean existsByCollaboratorIdAndYearAndMonth(
            Long collaboratorId,
            Integer year,
            Integer month
    );

    boolean existsByCollaboratorIdAndYearAndMonthAndIdNot(
            Long collaboratorId,
            Integer year,
            Integer month,
            Long id
    );

    List<Velocity> findByCollaborator_Team_IdAndYearAndMonth(
            Long teamId,
            Integer year,
            Integer month
    );
}