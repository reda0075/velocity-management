package com.velocity.velocity_management.ritual.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRitualRequest {

    @NotBlank
    @Size(max = 30, message = "Ritual name must not exceed 30 characters")
    private String name;

    @Positive(message = "Duration must be greater than 0")
    private Double durationHours;

}
