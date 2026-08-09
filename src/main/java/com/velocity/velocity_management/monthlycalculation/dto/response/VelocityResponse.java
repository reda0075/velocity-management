package com.velocity.velocity_management.monthlycalculation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VelocityResponse {

    private Long id;
    private Long collaboratorId;
    private Integer year;
    private Integer month;
    private Integer workingDays;
    private Double velocity;
    private List<VelocityRitualResponse> rituals;
    private Double totalRitualTimeHours;
    private Double ritualTimeDays;
    private Double effectiveWorkingDays;
    private Double velocityRatio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
