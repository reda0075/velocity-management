package com.velocity.velocity_management.teammonthlycalculation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamVelocityResponse {

    private Long id;
    private Long teamId;
    private String teamName;
    private Integer year;
    private Integer month;
    private Double teamVelocityRatio;
    private Integer totalMembers;
    private Integer validatedMembers;
    private Integer unvalidatedMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
