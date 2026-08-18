package com.velocity.velocity_management.teammonthlycalculation.repository;

import com.velocity.velocity_management.teammonthlycalculation.entity.TeamVelocity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamVelocityRepository extends JpaRepository<TeamVelocity, Long> {

    boolean existsByTeamIdAndYearAndMonth(
            Long teamId,
            Integer year,
            Integer month
    );

    Optional<TeamVelocity> findByTeamIdAndYearAndMonth(
            Long teamId,
            Integer year,
            Integer month
    );

    List<TeamVelocity> findByTeamId(Long teamId);
}
