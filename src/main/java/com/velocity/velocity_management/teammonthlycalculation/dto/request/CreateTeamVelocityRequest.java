package com.velocity.velocity_management.teammonthlycalculation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamVelocityRequest {

    @NotNull
    private Long teamId;

    @NotNull
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
}
