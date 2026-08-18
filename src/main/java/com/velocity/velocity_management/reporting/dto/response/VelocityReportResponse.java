package com.velocity.velocity_management.reporting.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VelocityReportResponse {

    private Long teamId;
    private String teamName;
    private Integer year;
    private Integer month;
    private int teamMembers;
    private int calculatedMembers;
    private double teamVelocity;
    private double averageIndividualVelocity;
    private int pendingValidations;
    private int validatedVelocities;

    private List<CollaboratorReportResponse> collaborators;
}