package com.velocity.velocity_management.collaborator.entity;


import com.velocity.velocity_management.collaborator.enums.Profile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "collaborators")
public class Collaborator {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false, unique = true, length = 30)
 private String matricule;

 @NotBlank
 @Size(max = 30)
 @Column(nullable = false, length = 30)
 private String firstName;

 @NotBlank
 @Size(max = 30)
 @Column(nullable = false, length = 30)
 private String lastName;

 @NotNull
 @Enumerated(EnumType.STRING)
 @Column(nullable = false)
 private Profile profile;

 @Column(nullable = false)
 private boolean active = true;

 @Column(nullable = false, updatable = false)
 private LocalDateTime createdAt;

 @Column(nullable = false)
 private LocalDateTime updatedAt;
}