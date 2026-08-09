package com.velocity.velocity_management.monthlycalculation.entity;

import com.velocity.velocity_management.ritual.entity.Ritual;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "velocity_rituals")
public class VelocityRitual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "velocity_id", nullable = false)
    private Velocity velocity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ritual_id", nullable = false)
    private Ritual ritual;

    @Column(nullable = false)
    private Integer occurrences;
}