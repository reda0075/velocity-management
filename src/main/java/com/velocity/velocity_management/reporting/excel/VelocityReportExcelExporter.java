package com.velocity.velocity_management.reporting.excel;

import com.velocity.velocity_management.reporting.dto.response.CollaboratorReportResponse;
import com.velocity.velocity_management.reporting.dto.response.VelocityReportResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Renders a {@link VelocityReportResponse} as a professional .xlsx workbook.
 * Every value is produced by {@code ReportingService}; this class only
 * formats the report, it never recalculates anything.
 */
@Component
public class VelocityReportExcelExporter {

    private static final String SUMMARY_SHEET_NAME = "Summary";
    private static final String COLLABORATORS_SHEET_NAME = "Collaborators";
    private static final String DECIMAL_PATTERN = "0.00";
    private static final String NO_CALCULATIONS_NOTE =
            "No velocity calculations for this period.";

    public byte[] export(VelocityReportResponse report) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildSummarySheet(workbook, report);
            buildCollaboratorsSheet(workbook, report);

            workbook.write(out);

            return out.toByteArray();
        } catch (IOException e) {

            throw new IllegalStateException(
                    "Could not generate the Excel velocity report", e);
        }
    }

    private void buildSummarySheet(Workbook workbook, VelocityReportResponse report) {

        Sheet sheet = workbook.createSheet(SUMMARY_SHEET_NAME);
        sheet.setColumnWidth(0, 32 * 256);
        sheet.setColumnWidth(1, 26 * 256);

        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle labelStyle = labelStyle(workbook);
        CellStyle decimalStyle = decimalStyle(workbook);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Velocity Report");
        titleCell.setCellStyle(titleStyle);

        Row headerRow = sheet.createRow(2);
        Cell metricHeader = headerRow.createCell(0);
        metricHeader.setCellValue("Metric");
        metricHeader.setCellStyle(headerStyle);
        Cell valueHeader = headerRow.createCell(1);
        valueHeader.setCellValue("Value");
        valueHeader.setCellStyle(headerStyle);

        int row = 3;

        row = textRow(sheet, row, "Team", report.getTeamName(), labelStyle);
        row = integerRow(sheet, row, "Year", report.getYear(), labelStyle);
        row = textRow(sheet, row, "Month", monthName(report.getMonth()), labelStyle);
        row = integerRow(sheet, row, "Active members", report.getTeamMembers(), labelStyle);
        row = integerRow(sheet, row, "Calculated members", report.getCalculatedMembers(), labelStyle);
        row = integerRow(sheet, row, "Missing calculations", missingCalculations(report), labelStyle);
        row = decimalRow(sheet, row, "Team velocity", report.getTeamVelocity(), labelStyle, decimalStyle);
        row = decimalRow(sheet, row, "Average individual velocity", report.getAverageIndividualVelocity(), labelStyle, decimalStyle);
        row = integerRow(sheet, row, "Pending validations", report.getPendingValidations(), labelStyle);
        row = integerRow(sheet, row, "Validated velocities", report.getValidatedVelocities(), labelStyle);
    }

    private void buildCollaboratorsSheet(Workbook workbook, VelocityReportResponse report) {

        Sheet sheet = workbook.createSheet(COLLABORATORS_SHEET_NAME);

        String[] headers = {
                "Matricule", "First name", "Last name", "Profile", "Velocity",
                "Ritual time hours", "Ritual time days", "Effective working days", "Status"
        };
        int[] widths = {26, 18, 18, 14, 12, 18, 16, 22, 20};

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        CellStyle headerStyle = headerStyle(workbook);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.createFreezePane(0, 1);

        List<CollaboratorReportResponse> collaborators =
                report.getCollaborators() == null ? List.of() : report.getCollaborators();

        if (collaborators.isEmpty()) {
            Row noteRow = sheet.createRow(1);
            noteRow.createCell(0).setCellValue(NO_CALCULATIONS_NOTE);
            return;
        }

        CellStyle decimalStyle = decimalStyle(workbook);

        int rowIndex = 1;

        for (CollaboratorReportResponse collaborator : collaborators) {

            Row row = sheet.createRow(rowIndex++);

            int column = 0;
            textCell(row, column++, collaborator.getMatricule());
            textCell(row, column++, collaborator.getFirstName());
            textCell(row, column++, collaborator.getLastName());
            textCell(row, column++, collaborator.getProfile());
            decimalCell(row, column++, collaborator.getVelocity(), decimalStyle);
            decimalCell(row, column++, collaborator.getRitualTimeHours(), decimalStyle);
            decimalCell(row, column++, collaborator.getRitualTimeDays(), decimalStyle);
            decimalCell(row, column++, collaborator.getEffectiveWorkingDays(), decimalStyle);
            textCell(row, column++, statusLabel(collaborator.getStatus()));
        }
    }

    private int textRow(Sheet sheet, int rowIndex, String label, String value, CellStyle labelStyle) {

        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value == null ? "" : value);

        return rowIndex + 1;
    }

    private int integerRow(Sheet sheet, int rowIndex, String label, int value, CellStyle labelStyle) {

        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value);

        return rowIndex + 1;
    }

    private int decimalRow(Sheet sheet, int rowIndex, String label, double value, CellStyle labelStyle, CellStyle decimalStyle) {

        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(decimalStyle);

        return rowIndex + 1;
    }

    private void textCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void decimalCell(Row row, int column, Double value, CellStyle decimalStyle) {

        Cell cell = row.createCell(column);

        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(decimalStyle);
        }
    }

    private CellStyle titleStyle(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle labelStyle(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }

    private CellStyle decimalStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(DECIMAL_PATTERN));

        return style;
    }

    private int missingCalculations(VelocityReportResponse report) {
        return Math.max(0, report.getTeamMembers() - report.getCalculatedMembers());
    }

    private String monthName(Integer month) {

        if (month == null) {
            return "";
        }

        if (month < 1 || month > 12) {
            return String.valueOf(month);
        }

        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private String statusLabel(String status) {

        if (status == null) {
            return "";
        }

        if ("VALIDATED".equals(status)) {
            return "Validated";
        }

        if ("PENDING_VALIDATION".equals(status)) {
            return "Pending validation";
        }

        return status;
    }
}
