package com.velocity.velocity_management.reporting.controller;

import com.velocity.velocity_management.reporting.dto.response.VelocityReportResponse;
import com.velocity.velocity_management.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/velocity")
    public ResponseEntity<VelocityReportResponse> getVelocityReport(
            @RequestParam Long teamId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        VelocityReportResponse report =
                reportingService.generateVelocityReport(
                        teamId,
                        year,
                        month
                );

        return ResponseEntity.ok(report);
    }
}