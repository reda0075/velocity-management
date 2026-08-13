package com.velocity.velocity_management.monthlycalculation.entity;


import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.monthlycalculation.enums.VelocityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "velocities")
public class Velocity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer workingDays;

    @Column(nullable = false)
    private Double velocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VelocityStatus status;

    @OneToMany(
            mappedBy = "velocity",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<VelocityRitual> rituals = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
