package com.velocity.velocity_management.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.velocity_management.collaborator.entity.Collaborator;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.team.entity.Team;
import com.velocity.velocity_management.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private TeamRepository teamRepository;

    private final String VALID_USERNAME = "admin";
    private final String VALID_PASSWORD = "admin123";
    private final String COLLABORATOR_USERNAME = "collaborator";
    private final String COLLABORATOR_PASSWORD = "collaborator123";

    @BeforeEach
    void assignTestCollaboratorToTeam() {
        Team team = teamRepository.findByName("Authorization Test Team")
                .orElseGet(() -> {
                    Team newTeam = new Team();
                    newTeam.setName("Authorization Test Team");
                    newTeam.setDescription("Team used by authorization integration tests");
                    newTeam.setActive(true);
                    return teamRepository.save(newTeam);
                });

        Collaborator collaborator = collaboratorRepository.findByUserUsername(COLLABORATOR_USERNAME)
                .orElseThrow();
        collaborator.setTeam(team);
        collaboratorRepository.save(collaborator);
    }

    @Test
    void login_WithValidCredentials_ReturnsJwtAndUserInfo() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(VALID_USERNAME);
        request.setPassword(VALID_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_WithCollaboratorCredentials_ReturnsJwtAndUserInfo() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(COLLABORATOR_USERNAME);
        request.setPassword(COLLABORATOR_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(COLLABORATOR_USERNAME))
                .andExpect(jsonPath("$.role").value("COLLABORATOR"));
    }

    @Test
    void login_WithInvalidPassword_Returns401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(VALID_USERNAME);
        request.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_WithNonExistentUser_Returns401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithMissingUsername_Returns400() throws Exception {
        String json = """
                {"password":"password"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithMissingPassword_Returns400() throws Exception {
        String json = """
                {"username":"admin"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithEmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_WithValidToken_Returns200() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(VALID_USERNAME);
        request.setPassword(VALID_PASSWORD);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/teams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void collaborator_CanAccessOnlyOwnResources() throws Exception {
        String token = loginAndGetToken(COLLABORATOR_USERNAME, COLLABORATOR_PASSWORD);

        mockMvc.perform(get("/api/collaborators/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"));

        mockMvc.perform(get("/api/teams/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Authorization Test Team"));

        mockMvc.perform(get("/api/velocities/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void collaborator_CannotAccessAdministrativeEndpoints() throws Exception {
        String token = loginAndGetToken(COLLABORATOR_USERNAME, COLLABORATOR_PASSWORD);

        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/velocities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Team\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/velocity?teamId=1&year=2026&month=9")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_RetainsAccessToAdministrativeEndpoints() throws Exception {
        String token = loginAndGetToken(VALID_USERNAME, VALID_PASSWORD);

        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/teams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/velocities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void ownResourceEndpoints_WithoutToken_Return401() throws Exception {
        mockMvc.perform(get("/api/collaborators/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/teams/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/velocities/me"))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
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
