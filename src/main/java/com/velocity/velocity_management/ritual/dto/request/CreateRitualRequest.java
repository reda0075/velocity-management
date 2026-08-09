package com.velocity.velocity_management.ritual.dto.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateRitualRequest {

    @NotBlank
    @Size(max = 30, message = "Ritual name must not exceed 30 characters")
    private String name;

    @Positive(message = "Duration must be greater than 0")
    private Double durationHours;


}
