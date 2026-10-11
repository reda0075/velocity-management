package com.velocity.velocity_management.reporting.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.velocity.velocity_management.reporting.dto.response.CollaboratorReportResponse;
import com.velocity.velocity_management.reporting.dto.response.VelocityReportResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Renders a {@link VelocityReportResponse} as a professional PDF report.
 * Every value is produced by {@code ReportingService}; this class only
 * formats the report, it never recalculates anything.
 */
@Component
public class VelocityReportPdfExporter {

    private static final Color DARK_BLUE = new Color(31, 58, 95);
    private static final Color LIGHT_ROW = new Color(242, 245, 249);
    private static final Color BORDER = new Color(198, 208, 222);
    private static final Color TEXT = new Color(44, 52, 64);

    private static final float SUMMARY_WIDTH = 240f;
    private static final String NO_CALCULATIONS_NOTE =
            "No velocity calculations for this period.";

    public byte[] export(VelocityReportResponse report) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document =
                    new Document(PageSize.A4, 48, 48, 64, 64);

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Footer());

            document.open();

            buildHeader(document, report);
            buildSummary(document, report);
            buildCollaborators(document, report);

            document.close();

            return out.toByteArray();
        } catch (IOException | DocumentException e) {

            throw new IllegalStateException(
                    "Could not generate the PDF velocity report", e);
        }
    }

    private void buildHeader(Document document, VelocityReportResponse report)
            throws DocumentException {

        Font title = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 20, Font.BOLD, DARK_BLUE);
        Font subtitle = FontFactory.getFont(
                FontFactory.HELVETICA, 11, Font.NORMAL, TEXT);

        Paragraph titleParagraph = new Paragraph("Velocity Report", title);
        titleParagraph.setSpacingAfter(4);
        document.add(titleParagraph);

        Paragraph subtitleParagraph = new Paragraph(
                report.getTeamName() + " — " + periodLabel(report), subtitle);
        subtitleParagraph.setSpacingAfter(14);
        document.add(subtitleParagraph);
    }

    private void buildSummary(Document document, VelocityReportResponse report)
            throws DocumentException {

        Font section = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 13, Font.BOLD, DARK_BLUE);
        document.add(new Paragraph("Summary", section));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidths(new float[]{150f, SUMMARY_WIDTH - 150f});
        table.setTotalWidth(SUMMARY_WIDTH);
        table.setLockedWidth(true);
        table.setSpacingAfter(18);

        addSummaryRow(table, "Team", text(report.getTeamName()), true);
        addSummaryRow(table, "Period", periodLabel(report), true);
        addSummaryRow(table, "Team velocity",
                decimal(report.getTeamVelocity()), false);
        addSummaryRow(table, "Active members",
                String.valueOf(report.getTeamMembers()), false);
        addSummaryRow(table, "Calculated members",
                String.valueOf(report.getCalculatedMembers()), false);
        addSummaryRow(table, "Missing calculations",
                String.valueOf(missingCalculations(report)), false);
        addSummaryRow(table, "Pending validations",
                String.valueOf(report.getPendingValidations()), false);
        addSummaryRow(table, "Validated velocities",
                String.valueOf(report.getValidatedVelocities()), false);

        document.add(table);
    }

    private void addSummaryRow(
            PdfPTable table, String label, String value, boolean boldValue) {

        Font labelFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 10, Font.BOLD, TEXT);
        Font valueFont = FontFactory.getFont(
                FontFactory.HELVETICA, 10,
                boldValue ? Font.BOLD : Font.NORMAL, TEXT);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        styleCell(labelCell, false);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        styleCell(valueCell, false);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void buildCollaborators(Document document, VelocityReportResponse report)
            throws DocumentException {

        Font section = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 13, Font.BOLD, DARK_BLUE);
        document.add(new Paragraph("Collaborator details", section));
        document.add(new Paragraph(" "));

        List<CollaboratorReportResponse> collaborators =
                report.getCollaborators() == null ? List.of() : report.getCollaborators();

        if (collaborators.isEmpty()) {
            Font note = FontFactory.getFont(
                    FontFactory.HELVETICA_OBLIQUE, 10, Font.ITALIC, TEXT);
            document.add(new Paragraph(NO_CALCULATIONS_NOTE, note));
            return;
        }

        String[] headers = {
                "Matricule", "First name", "Last name", "Profile", "Velocity",
                "Ritual time hours", "Ritual time days", "Effective working days",
                "Status"
        };
        float[] widths = {
                62f, 48f, 48f, 40f, 42f, 52f, 48f, 52f, 58f
        };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidths(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(8);

        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 8, Font.BOLD, Color.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            styleCell(cell, true);
            cell.setBackgroundColor(DARK_BLUE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        Font bodyFont = FontFactory.getFont(
                FontFactory.HELVETICA, 8, Font.NORMAL, TEXT);
        Font numericFont = FontFactory.getFont(
                FontFactory.HELVETICA, 8, Font.NORMAL, TEXT);

        int rowIndex = 0;
        for (CollaboratorReportResponse collaborator : collaborators) {

            boolean shaded = rowIndex % 2 == 1;

            addCell(table, text(collaborator.getMatricule()), bodyFont,
                    Element.ALIGN_LEFT, shaded);
            addCell(table, text(collaborator.getFirstName()), bodyFont,
                    Element.ALIGN_LEFT, shaded);
            addCell(table, text(collaborator.getLastName()), bodyFont,
                    Element.ALIGN_LEFT, shaded);
            addCell(table, text(collaborator.getProfile()), bodyFont,
                    Element.ALIGN_LEFT, shaded);
            addCell(table, decimal(collaborator.getVelocity()), numericFont,
                    Element.ALIGN_RIGHT, shaded);
            addCell(table, decimal(collaborator.getRitualTimeHours()), numericFont,
                    Element.ALIGN_RIGHT, shaded);
            addCell(table, decimal(collaborator.getRitualTimeDays()), numericFont,
                    Element.ALIGN_RIGHT, shaded);
            addCell(table, decimal(collaborator.getEffectiveWorkingDays()), numericFont,
                    Element.ALIGN_RIGHT, shaded);
            addCell(table, statusLabel(collaborator.getStatus()), bodyFont,
                    Element.ALIGN_LEFT, shaded);

            rowIndex++;
        }

        document.add(table);
    }

    private void addCell(PdfPTable table, String value, Font font,
                         int alignment, boolean shaded) {

        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        styleCell(cell, false);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (shaded) {
            cell.setBackgroundColor(LIGHT_ROW);
        }
        table.addCell(cell);
    }

    private void styleCell(PdfPCell cell, boolean header) {
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(5);
        if (!header) {
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }
    }

    private int missingCalculations(VelocityReportResponse report) {
        return Math.max(0, report.getTeamMembers() - report.getCalculatedMembers());
    }

    private String periodLabel(VelocityReportResponse report) {
        return monthName(report.getMonth()) + " " + report.getYear();
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

    private String decimal(Double value) {
        return value == null
                ? "—"
                : String.format(Locale.US, "%.2f", value);
    }

    private String text(String value) {
        return value == null ? "" : value;
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

    private static class Footer extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            Font footerFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 8, Font.NORMAL, TEXT);

            int pageNumber = writer.getPageNumber();
            Phrase phrase = new Phrase(
                    "Velocity Report — page " + pageNumber, footerFont);

            float x = document.left() + 10;
            float y = document.bottom() - 18;

            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContentUnder(),
                    Element.ALIGN_LEFT,
                    phrase,
                    x,
                    y,
                    0);
        }
    }
}
