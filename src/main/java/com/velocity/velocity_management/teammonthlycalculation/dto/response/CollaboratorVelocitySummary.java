package com.velocity.velocity_management.teammonthlycalculation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorVelocitySummary {

    private Long collaboratorId;
    private String collaboratorName;

    /** Null when this collaborator has no velocity calculation for the period at all. */
    private Double velocityRatio;

    /** One of: VALIDATED, PENDING_VALIDATION, NOT_CALCULATED */
    private String status;

    /** Whether this collaborator's ratio was actually included in the team average. */
    private boolean includedInCalculation;
}
