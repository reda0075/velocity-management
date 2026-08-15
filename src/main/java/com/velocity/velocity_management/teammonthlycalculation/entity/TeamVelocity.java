package com.velocity.velocity_management.teammonthlycalculation.entity;

import com.velocity.velocity_management.team.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "team_velocities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_velocity_period",
                        columnNames = {"team_id", "year", "month"}
                )
        })
public class TeamVelocity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Double teamVelocityRatio;

    @Column(nullable = false)
    private Integer totalMembers;

    @Column(nullable = false)
    private Integer validatedMembers;

    @Column(nullable = false)
    private Integer unvalidatedMembers;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
