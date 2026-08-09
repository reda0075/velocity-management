package com.velocity.velocity_management.monthlycalculation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VelocityRitualRequest {

    @NotNull(message = "Ritual ID is required")
    @Positive(message = "Ritual ID must be greater than 0")
    private Long ritualId;

    @NotNull(message = "Occurrences are required")
    @Positive(message = "Occurrences must be greater than 0")
    private Integer occurrences;
}
