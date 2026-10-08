package com.velocity.velocity_management.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.enums.Profile;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.entity.VelocityRitual;
import com.velocity.velocity_management.monthlycalculation.enums.VelocityStatus;
import com.velocity.velocity_management.monthlycalculation.repository.VelocityRepository;
import com.velocity.velocity_management.ritual.entity.Ritual;
import com.velocity.velocity_management.ritual.repository.RitualRepository;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VelocityReportExcelExportTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String COLLABORATOR_USERNAME = "collaborator";
    private static final String COLLABORATOR_PASSWORD = "collaborator123";

    private static final int YEAR = 2026;
    private static final int MONTH = 9;
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private VelocityRepository velocityRepository;

    @Autowired
    private RitualRepository ritualRepository;

    @Test
    void validTeamAndMonth_ReturnsExcelWithSummaryAndCollaboratorRows() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Excel Valid Team " + suffix());
        Ritual ritual = createRitual("Daily Standup", 2.0);

        Collaborator first = createCollaborator("E.XCEL1_DEV", "Youssef", "Amrani", Profile.DEV, team);
        createVelocity(first, YEAR, MONTH, 120.0, 20, VelocityStatus.VALIDATED, ritual, 4);

        // A calculation from another month must not leak into the report.
        createVelocity(first, YEAR, MONTH - 1, 999.0, 20, VelocityStatus.VALIDATED, ritual, 1);

        Collaborator second = createCollaborator("E.XCEL2_QA", "Sara", "Bennani", Profile.QA, team);
        createVelocity(second, YEAR, MONTH, 90.0, 20, VelocityStatus.PENDING_VALIDATION, ritual, 2);

        String expectedFilename = "velocity-report-"
                + team.getName().replace(" ", "-") + "-" + YEAR + "-" + MONTH + ".xlsx";

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + expectedFilename + "\""))
                .andReturn();

        byte[] excel = result.getResponse().getContentAsByteArray();
        assertThat(excel).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {

            Sheet summary = workbook.getSheet("Summary");
            assertThat(summary).isNotNull();

            assertThat(summaryText(summary, "Team")).isEqualTo(team.getName());
            assertThat(summaryNumber(summary, "Year")).isEqualTo(YEAR);
            assertThat(summaryText(summary, "Month")).isEqualTo("September");
            assertThat(summaryNumber(summary, "Active members")).isEqualTo(2);
            assertThat(summaryNumber(summary, "Calculated members")).isEqualTo(2);
            assertThat(summaryNumber(summary, "Missing calculations")).isEqualTo(0);
            assertThat(summaryNumber(summary, "Team velocity")).isCloseTo(105.0, within(0.001));
            assertThat(summaryNumber(summary, "Average individual velocity")).isCloseTo(105.0, within(0.001));
            assertThat(summaryNumber(summary, "Pending validations")).isEqualTo(1);
            assertThat(summaryNumber(summary, "Validated velocities")).isEqualTo(1);

            Sheet collaborators = workbook.getSheet("Collaborators");
            assertThat(collaborators).isNotNull();
            assertThat(collaborators.getLastRowNum()).isEqualTo(2);

            Row firstRow = collaborators.getRow(1);
            assertThat(cellText(firstRow, 0)).isEqualTo("E.XCEL1_DEV");
            assertThat(cellText(firstRow, 1)).isEqualTo("Youssef");
            assertThat(cellText(firstRow, 2)).isEqualTo("Amrani");
            assertThat(cellText(firstRow, 3)).isEqualTo("DEV");
            assertThat(cellNumber(firstRow, 4)).isCloseTo(120.0, within(0.001));
            assertThat(cellNumber(firstRow, 5)).isCloseTo(8.0, within(0.001));
            assertThat(cellNumber(firstRow, 6)).isCloseTo(1.0, within(0.001));
            assertThat(cellNumber(firstRow, 7)).isCloseTo(19.0, within(0.001));
            assertThat(cellText(firstRow, 8)).isEqualTo("Validated");

            Row secondRow = collaborators.getRow(2);
            assertThat(cellText(secondRow, 0)).isEqualTo("E.XCEL2_QA");
            assertThat(cellText(secondRow, 1)).isEqualTo("Sara");
            assertThat(cellText(secondRow, 2)).isEqualTo("Bennani");
            assertThat(cellText(secondRow, 3)).isEqualTo("QA");
            assertThat(cellNumber(secondRow, 4)).isCloseTo(90.0, within(0.001));
            assertThat(cellNumber(secondRow, 5)).isCloseTo(4.0, within(0.001));
            assertThat(cellNumber(secondRow, 6)).isCloseTo(0.5, within(0.001));
            assertThat(cellNumber(secondRow, 7)).isCloseTo(19.5, within(0.001));
            assertThat(cellText(secondRow, 8)).isEqualTo("Pending validation");
        }
    }

    @Test
    void teamWithoutCalculations_ReturnsExcelWithZeroCalculatedMembers() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Excel Empty Team " + suffix());
        createCollaborator("E.EMPTY1_DEV", "No", "Report", Profile.DEV, team);
        createCollaborator("E.EMPTY2_DO", "Also", "Idle", Profile.DEVOPS, team);

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_CONTENT_TYPE))
                .andReturn();

        byte[] excel = result.getResponse().getContentAsByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {

            Sheet summary = workbook.getSheet("Summary");
            assertThat(summary).isNotNull();
            assertThat(summaryText(summary, "Team")).isEqualTo(team.getName());
            assertThat(summaryNumber(summary, "Active members")).isEqualTo(2);
            assertThat(summaryNumber(summary, "Calculated members")).isEqualTo(0);
            assertThat(summaryNumber(summary, "Missing calculations")).isEqualTo(2);
            assertThat(summaryNumber(summary, "Team velocity")).isEqualTo(0.0);
            assertThat(summaryNumber(summary, "Average individual velocity")).isEqualTo(0.0);
            assertThat(summaryNumber(summary, "Pending validations")).isEqualTo(0);
            assertThat(summaryNumber(summary, "Validated velocities")).isEqualTo(0);

            Sheet collaborators = workbook.getSheet("Collaborators");
            assertThat(collaborators).isNotNull();
            // Header row plus the "no calculations" note row.
            assertThat(collaborators.getLastRowNum()).isEqualTo(1);
            assertThat(cellText(collaborators.getRow(1), 0))
                    .isEqualTo("No velocity calculations for this period.");
        }
    }

    @Test
    void multipleCollaborators_AreAllListedInTheCollaboratorsSheet() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Excel Multi Team " + suffix());
        Ritual ritual = createRitual("Sprint Planning", 4.0);

        Collaborator first = createCollaborator("E.MULTI1_DEV", "Amina", "Zeroual", Profile.DEV, team);
        createVelocity(first, YEAR, MONTH, 100.0, 22, VelocityStatus.VALIDATED, ritual, 2);

        Collaborator second = createCollaborator("E.MULTI2_DO", "Omar", "Fassi", Profile.DEVOPS, team);
        createVelocity(second, YEAR, MONTH, 110.0, 22, VelocityStatus.PENDING_VALIDATION, ritual, 1);

        Collaborator third = createCollaborator("E.MULTI3_QA", "Laila", "Tazi", Profile.QA, team);
        createVelocity(third, YEAR, MONTH, 95.0, 22, VelocityStatus.VALIDATED, ritual, 3);

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        byte[] excel = result.getResponse().getContentAsByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {

            Sheet collaborators = workbook.getSheet("Collaborators");
            assertThat(collaborators).isNotNull();
            assertThat(collaborators.getLastRowNum()).isEqualTo(3);

            assertThat(cellText(collaborators.getRow(1), 0)).isEqualTo("E.MULTI1_DEV");
            assertThat(cellText(collaborators.getRow(2), 0)).isEqualTo("E.MULTI2_DO");
            assertThat(cellText(collaborators.getRow(3), 0)).isEqualTo("E.MULTI3_QA");

            // Statuses keep their readable labels for every row.
            assertThat(cellText(collaborators.getRow(1), 8)).isEqualTo("Validated");
            assertThat(cellText(collaborators.getRow(2), 8)).isEqualTo("Pending validation");
            assertThat(cellText(collaborators.getRow(3), 8)).isEqualTo("Validated");

            // Team velocity is the average of the three calculations.
            Sheet summary = workbook.getSheet("Summary");
            assertThat(summaryNumber(summary, "Team velocity")).isCloseTo(101.6667, within(0.001));
        }
    }

    @Test
    void invalidTeamId_Returns404() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", "987654321")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", "1")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collaboratorToken_IsForbidden() throws Exception {
        String token = login(COLLABORATOR_USERNAME, COLLABORATOR_PASSWORD);

        mockMvc.perform(get("/api/reports/velocity/export/excel")
                        .param("teamId", "1")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Team createTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setDescription("Team used by Excel export tests");
        team.setActive(true);
        return teamRepository.save(team);
    }

    private Collaborator createCollaborator(
            String matricule, String firstName, String lastName, Profile profile, Team team) {

        Collaborator collaborator = new Collaborator();
        collaborator.setMatricule(matricule);
        collaborator.setFirstName(firstName);
        collaborator.setLastName(lastName);
        collaborator.setProfile(profile);
        collaborator.setActive(true);
        collaborator.setTeam(team);
        collaborator.setCreatedAt(LocalDateTime.now());
        collaborator.setUpdatedAt(LocalDateTime.now());

        return collaboratorRepository.save(collaborator);
    }

    private Ritual createRitual(String name, double durationHours) {
        Ritual ritual = new Ritual();
        ritual.setName(name + " " + suffix());
        ritual.setDurationHours(durationHours);
        ritual.setCreatedAt(LocalDateTime.now());
        ritual.setUpdatedAt(LocalDateTime.now());
        return ritualRepository.save(ritual);
    }

    private void createVelocity(
            Collaborator collaborator,
            int year,
            int month,
            double velocity,
            int workingDays,
            VelocityStatus status,
            Ritual ritual,
            int occurrences) {

        Velocity entity = new Velocity();
        entity.setCollaborator(collaborator);
        entity.setYear(year);
        entity.setMonth(month);
        entity.setWorkingDays(workingDays);
        entity.setVelocity(velocity);
        entity.setStatus(status);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        VelocityRitual velocityRitual = new VelocityRitual();
        velocityRitual.setVelocity(entity);
        velocityRitual.setRitual(ritual);
        velocityRitual.setOccurrences(occurrences);
        entity.getRituals().add(velocityRitual);

        velocityRepository.save(entity);
    }

    private String login(String username, String password) throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private int summaryRow(Sheet summary, String label) {
        for (Row row : summary) {
            Cell cell = row.getCell(0);
            if (cell != null && label.equals(cell.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("Summary row not found: " + label);
    }

    private String summaryText(Sheet summary, String label) {
        Cell cell = summary.getRow(summaryRow(summary, label)).getCell(1);
        assertThat(cell).isNotNull();
        return cell.getStringCellValue();
    }

    private double summaryNumber(Sheet summary, String label) {
        Cell cell = summary.getRow(summaryRow(summary, label)).getCell(1);
        assertThat(cell).isNotNull();
        return cell.getNumericCellValue();
    }

    private String cellText(Row row, int column) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : cell.getStringCellValue();
    }

    private double cellNumber(Row row, int column) {
        return row.getCell(column).getNumericCellValue();
    }
}
