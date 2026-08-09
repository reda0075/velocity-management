package com.velocity.velocity_management.monthlycalculation.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class CreateVelocityRequest {

        @NotNull(message = "Collaborator ID is required")
        @Positive(message = "Collaborator ID must be greater than 0")
        private Long collaboratorId;

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be valid")
        private Integer year;

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        private Integer month;

        @NotNull(message = "Working days are required")
        @Positive(message = "Working days must be greater than 0")
        private Integer workingDays;

        @NotNull(message = "Velocity is required")
        @Positive(message = "Velocity must be greater than 0")
        private Double velocity;

        @NotNull(message = "Rituals are required")
        @Valid
        private List<VelocityRitualRequest> rituals;
    }

