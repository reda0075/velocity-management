package com.velocity.velocity_management.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VelocityReportPdfExportTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String COLLABORATOR_USERNAME = "collaborator";
    private static final String COLLABORATOR_PASSWORD = "collaborator123";

    private static final int YEAR = 2026;
    private static final int MONTH = 9;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

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
    void validTeamAndMonth_ReturnsPdfWithReportData() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("PDF Valid Team " + suffix());
        Ritual ritual = createRitual("Daily Standup", 2.0);

        Collaborator first = createCollaborator("PDF1_DEV", "Youssef", "Amrani", Profile.DEV, team);
        createVelocity(first, YEAR, MONTH, 120.0, 20, VelocityStatus.VALIDATED, ritual, 4);

        // A calculation from another month must not leak into the report.
        createVelocity(first, YEAR, MONTH - 1, 999.0, 20, VelocityStatus.VALIDATED, ritual, 1);

        Collaborator second = createCollaborator("PDF2_QA", "Sara", "Bennani", Profile.QA, team);
        createVelocity(second, YEAR, MONTH, 90.0, 20, VelocityStatus.PENDING_VALIDATION, ritual, 2);

        String expectedFilename = "velocity-report-"
                + team.getName().replace(" ", "-") + "-" + YEAR + "-" + MONTH + ".pdf";

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", PDF_CONTENT_TYPE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + expectedFilename + "\""))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();

        String text = pdfText(pdf);

        // Header and summary section
        assertThat(text).contains("Velocity Report");
        assertThat(text).contains(team.getName());
        assertThat(text).contains("September 2026");
        assertThat(text).contains("Team velocity");
        assertThat(text).contains("105.00");
        assertThat(text).contains("Active members");
        assertThat(text).contains("Calculated members");
        assertThat(text).contains("Missing calculations");
        assertThat(text).contains("Pending validations");
        assertThat(text).contains("Validated velocities");

        // Collaborator details
        assertThat(text).contains("Collaborator details");
        assertThat(text).contains("PDF1_DEV");
        assertThat(text).contains("Youssef");
        assertThat(text).contains("Amrani");
        assertThat(text).contains("DEV");
        assertThat(text).contains("120.00");
        assertThat(text).contains("8.00");
        assertThat(text).contains("19.00");
        assertThat(text).contains("Validated");

        assertThat(text).contains("PDF2_QA");
        assertThat(text).contains("Sara");
        assertThat(text).contains("Bennani");
        assertThat(text).contains("QA");
        assertThat(text).contains("90.00");
        assertThat(text).contains("4.00");
        assertThat(text).contains("19.50");
        assertThat(text).contains("Pending validation");
    }

    @Test
    void teamWithoutCalculations_ReturnsPdfWithZeroCalculatedMembers() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("PDF Empty Team " + suffix());
        createCollaborator("PDFE1_DEV", "No", "Report", Profile.DEV, team);
        createCollaborator("PDFE2_DO", "Also", "Idle", Profile.DEVOPS, team);

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", PDF_CONTENT_TYPE))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();

        String text = pdfText(pdf);

        assertThat(text).contains(team.getName());
        assertThat(text).contains("Active members");
        assertThat(text).contains("Calculated members");
        assertThat(text).contains("Missing calculations");
        assertThat(text).contains("No velocity calculations for this period.");
        // No collaborator rows without calculations.
        assertThat(text).doesNotContain("PDFE1_DEV");
        assertThat(text).doesNotContain("PDFE2_DO");
    }

    @Test
    void multipleCollaborators_AreAllListedInThePdf() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("PDF Multi Team " + suffix());
        Ritual ritual = createRitual("Sprint Planning", 4.0);

        Collaborator first = createCollaborator("PDFM1_DEV", "Amina", "Zeroual", Profile.DEV, team);
        createVelocity(first, YEAR, MONTH, 100.0, 22, VelocityStatus.VALIDATED, ritual, 2);

        Collaborator second = createCollaborator("PDFM2_DO", "Omar", "Fassi", Profile.DEVOPS, team);
        createVelocity(second, YEAR, MONTH, 110.0, 22, VelocityStatus.PENDING_VALIDATION, ritual, 1);

        Collaborator third = createCollaborator("PDFM3_QA", "Laila", "Tazi", Profile.QA, team);
        createVelocity(third, YEAR, MONTH, 95.0, 22, VelocityStatus.VALIDATED, ritual, 3);

        MvcResult result = mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", team.getId().toString())
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", PDF_CONTENT_TYPE))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();

        String text = pdfText(pdf);

        // Every collaborator row is present with its readable status label.
        assertThat(text).contains("PDFM1_DEV");
        assertThat(text).contains("PDFM2_DO");
        assertThat(text).contains("PDFM3_QA");
        assertThat(text).contains("Amina");
        assertThat(text).contains("Omar");
        assertThat(text).contains("Laila");

        // Team velocity is the average of the three calculations.
        assertThat(text).contains("101.67");
    }

    @Test
    void invalidTeamId_Returns404() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", "987654321")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", "1")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collaboratorToken_IsForbidden() throws Exception {
        String token = login(COLLABORATOR_USERNAME, COLLABORATOR_PASSWORD);

        mockMvc.perform(get("/api/reports/velocity/export/pdf")
                        .param("teamId", "1")
                        .param("year", String.valueOf(YEAR))
                        .param("month", String.valueOf(MONTH))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String pdfText(byte[] pdf) throws Exception {
        assertThat(pdf).startsWith("%PDF-".getBytes());

        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }
            return text.toString();
        }
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Team createTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setDescription("Team used by PDF export tests");
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
}
