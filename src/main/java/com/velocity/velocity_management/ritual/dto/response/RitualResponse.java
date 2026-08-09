package com.velocity.velocity_management.ritual.dto.response;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RitualResponse {


    private Long id;
    private String name;
    private Double durationHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
