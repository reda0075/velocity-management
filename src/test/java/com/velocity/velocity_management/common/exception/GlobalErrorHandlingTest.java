package com.velocity.velocity_management.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.enums.Profile;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.ritual.entity.Ritual;
import com.velocity.velocity_management.ritual.repository.RitualRepository;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the API distinguishes 400 / 401 / 403 / 404 / 409 / 500
 * and returns meaningful messages in the stable ApiError format.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalErrorHandlingTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String COLLABORATOR_USERNAME = "collaborator";
    private static final String COLLABORATOR_PASSWORD = "collaborator123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private RitualRepository ritualRepository;

    // ---- 404 Not Found ----

    @Test
    void missingResource_Returns404WithMeaningfulMessage() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(get("/api/velocities/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("message").asText())
                .isEqualTo("Velocity ID 999999 not found");
    }

    // ---- 409 Conflict ----

    @Test
    void duplicateVelocity_Returns409WithMeaningfulMessage() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Err Conflict Team " + suffix());
        Collaborator collaborator =
                createCollaborator("ERRCF_DEV", "Err", "Conflict", Profile.DEV, team);

        String first = objectMapper.writeValueAsString(velocityPayload(
                collaborator.getId(), 2026, 11, 20, 100.0));

        mockMvc.perform(post("/api/velocities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/velocities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(body.get("message").asText())
                .isEqualTo("A velocity calculation already exists for this collaborator and month");
    }

    @Test
    void duplicateTeamName_Returns409WithMeaningfulMessage() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String name = "Err Duplicate Team " + suffix();

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(body.get("message").asText())
                .isEqualTo("A team with this name already exists");
    }

    @Test
    void duplicateRitualName_Returns409WithMeaningfulMessage() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String name = "Err Ritual " + suffix();

        mockMvc.perform(post("/api/rituals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"durationHours\":2.0}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/rituals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"durationHours\":2.0}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(body.get("message").asText()).contains("already exists");
    }

    // ---- 400 Bad Request ----

    @Test
    void teamVelocityForTeamWithoutMembers_Returns400WithMeaningfulMessage()
            throws Exception {

        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Err Empty Team " + suffix());

        MvcResult result = mockMvc.perform(post("/api/team-velocities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":" + team.getId()
                                + ",\"year\":2026,\"month\":11}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText())
                .isEqualTo("This team has no active collaborators");
    }

    @Test
    void teamVelocityWithoutValidatedVelocities_Returns400WithMeaningfulMessage()
            throws Exception {

        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Team team = createTeam("Err NoValidated Team " + suffix());
        createCollaborator("ERRNV_DEV", "Err", "NoValidated", Profile.DEV, team);

        MvcResult result = mockMvc.perform(post("/api/team-velocities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":" + team.getId()
                                + ",\"year\":2026,\"month\":11}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText())
                .isEqualTo("No validated velocity calculations found for this team in this period");
    }

    @Test
    void malformedJson_Returns400WithoutStackTrace() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).isEqualTo("Malformed request body");
        String raw = result.getResponse().getContentAsString();
        assertThat(raw).doesNotContain("Exception").doesNotContain("at com.velocity");
    }

    @Test
    void missingRequiredParameter_Returns400NamingTheParameter() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(get("/api/reports/velocity")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText())
                .isEqualTo("Missing required parameter: teamId");
    }

    @Test
    void invalidParameterType_Returns400() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(get("/api/velocities/abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).isEqualTo("Invalid value for parameter: id");
    }

    @Test
    void validationFailure_KeepsFieldErrorsFormat() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).isEqualTo("Validation failed");
        assertThat(body.get("errors").get("name").asText())
                .isEqualTo("Team name is required");
    }

    // ---- 401 / 403 ----

    @Test
    void unauthorizedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collaboratorOnAdminEndpoint_Returns403() throws Exception {
        String token = login(COLLABORATOR_USERNAME, COLLABORATOR_PASSWORD);

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Should not be created\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Team createTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setDescription("Team used by error handling tests");
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

    private java.util.Map<String, Object> velocityPayload(
            Long collaboratorId, int year, int month, int workingDays, double velocity) {

        Ritual ritual = ritualRepository.findAll().isEmpty()
                ? createDefaultRitual()
                : ritualRepository.findAll().get(0);

        java.util.Map<String, Object> ritualRequest = new java.util.HashMap<>();
        ritualRequest.put("ritualId", ritual.getId());
        ritualRequest.put("occurrences", 1);

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("collaboratorId", collaboratorId);
        payload.put("year", year);
        payload.put("month", month);
        payload.put("workingDays", workingDays);
        payload.put("velocity", velocity);
        payload.put("rituals", java.util.Collections.singletonList(ritualRequest));

        return payload;
    }

    private Ritual createDefaultRitual() {
        Ritual ritual = new Ritual();
        ritual.setName("Error Test Ritual " + suffix());
        ritual.setDurationHours(1.0);
        ritual.setCreatedAt(LocalDateTime.now());
        ritual.setUpdatedAt(LocalDateTime.now());
        return ritualRepository.save(ritual);
    }

    private String login(String username, String password) throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
