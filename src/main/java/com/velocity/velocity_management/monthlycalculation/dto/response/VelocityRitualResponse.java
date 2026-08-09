package com.velocity.velocity_management.monthlycalculation.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VelocityRitualResponse {

    private Long ritualId;
    private String ritualName;
    private Double durationHours;
    private Integer occurrences;
    private Double totalTimeHours;
}
