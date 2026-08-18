package com.velocity.velocity_management.reporting.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollaboratorReportResponse {

    private Long collaboratorId;
    private String matricule;
    private String firstName;
    private String lastName;
    private String profile;

    private Double velocity;

    private Double ritualTimeHours;
    private Double ritualTimeDays;
    private Double effectiveWorkingDays;

    private String status;
}