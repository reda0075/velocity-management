package com.velocity.velocity_management.reporting.controller;

import com.velocity.velocity_management.reporting.dto.response.VelocityReportResponse;
import com.velocity.velocity_management.reporting.excel.VelocityReportExcelExporter;
import com.velocity.velocity_management.reporting.service.ReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportingService reportingService;
    private final VelocityReportExcelExporter excelExporter;

    public ReportingController(
            ReportingService reportingService,
            VelocityReportExcelExporter excelExporter) {

        this.reportingService = reportingService;
        this.excelExporter = excelExporter;
    }

    @GetMapping("/velocity")
    public ResponseEntity<VelocityReportResponse> getVelocityReport(
            @RequestParam("teamId") Long teamId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {

        VelocityReportResponse report =
                reportingService.generateVelocityReport(
                        teamId,
                        year,
                        month
                );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/velocity/export/excel")
    public ResponseEntity<byte[]> exportVelocityReportExcel(
            @RequestParam("teamId") Long teamId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {

        // The report is calculated by the same service as the JSON endpoint:
        // this endpoint only renders the result as an Excel workbook.
        VelocityReportResponse report =
                reportingService.generateVelocityReport(
                        teamId,
                        year,
                        month
                );

        byte[] excel = excelExporter.export(report);

        return ResponseEntity.ok()
                .contentType(EXCEL_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + excelFilename(report) + "\"")
                .body(excel);
    }

    private String excelFilename(VelocityReportResponse report) {

        String team = report.getTeamName() == null
                ? "team"
                : report.getTeamName().replaceAll("[^a-zA-Z0-9_-]", "-");

        return "velocity-report-" + team + "-" + report.getYear() + "-" + report.getMonth() + ".xlsx";
    }
}
