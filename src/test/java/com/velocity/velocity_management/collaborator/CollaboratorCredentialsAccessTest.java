package com.velocity.velocity_management.collaborator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.auth.entity.User;
import com.velocity.velocity_management.auth.repository.UserRepository;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollaboratorCredentialsAccessTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String SEEDED_COLLABORATOR_USERNAME = "collaborator";
    private static final String SEEDED_COLLABORATOR_PASSWORD = "collaborator123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void admin_CanRetrieveCredentialsOfAnExistingCollaborator() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        var created = objectMapper.readTree(createCollaborator(token));
        long id = created.get("id").asLong();

        String matricule = created.get("matricule").asText();
        String issuedPassword = created.at("/initialCredentials/temporaryPassword").asText();

        String body = mockMvc.perform(get("/api/collaborators/{id}/credentials", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(matricule))
                .andExpect(jsonPath("$.temporaryPassword").value(issuedPassword))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Nothing beyond the two documented fields may leak.
        assertThat(objectMapper.readTree(body).fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("username", "temporaryPassword");
    }

    @Test
    void retrievedPassword_IsTheOneIssuedAtCreation_AndStillAuthenticates() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        var created = objectMapper.readTree(createCollaborator(token));
        long id = created.get("id").asLong();
        String matricule = created.get("matricule").asText();

        String body = mockMvc.perform(get("/api/collaborators/{id}/credentials", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String username = objectMapper.readTree(body).get("username").asText();
        String password = objectMapper.readTree(body).get("temporaryPassword").asText();

        // The recovered password must still authenticate, proving BCrypt was not replaced.
        login(username, password);

        assertThat(username).isEqualTo(matricule);
        assertThat(password).matches("^" + Pattern.quote(matricule) + "_[0-9]{4}$");
    }

    @Test
    void collaborator_CannotRetrieveCredentials() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        var created = objectMapper.readTree(createCollaborator(adminToken));
        long id = created.get("id").asLong();

        String collaboratorToken = login(created.get("matricule").asText(),
                created.at("/initialCredentials/temporaryPassword").asText());

        // Access must be denied and no credential material may come back. The exact status is
        // asserted as 4xx because this application answers a denied collaborator with 401 in a
        // live run (its entry point answers both failures) while MockMvc yields 403. That quirk
        // predates this feature and applies to every ADMIN-only endpoint; see the report.
        mockMvc.perform(get("/api/collaborators/{id}/credentials", id)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist());

        // Same pre-existing behaviour for an endpoint that already existed.
        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void unauthenticatedRequest_IsRejected() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        long id = objectMapper.readTree(createCollaborator(adminToken)).get("id").asLong();

        mockMvc.perform(get("/api/collaborators/{id}/credentials", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void normalResponses_DoNotExposeAnyPasswordMaterial() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        var created = objectMapper.readTree(createCollaborator(token));
        long id = created.get("id").asLong();

        String list = mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain("temporaryPassword", "encryptedInitialPassword");

        String detail = mockMvc.perform(get("/api/collaborators/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialCredentials").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).doesNotContain("temporaryPassword", "encryptedInitialPassword", "password");

        String updated = mockMvc.perform(put("/api/collaborators/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Rachid","lastName":"Bennani","profile":"QA","active":true}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(updated).doesNotContain("temporaryPassword", "encryptedInitialPassword", "password");
    }

    @Test
    void collaborator_WithoutRecoverableCredentials_Returns404_AndNothingIsReset() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // The Phase 2 seeded user predates encrypted credential storage.
        User seeded = userRepository.findByUsername(SEEDED_COLLABORATOR_USERNAME).orElseThrow();
        assertThat(seeded.getEncryptedInitialPassword()).isNull();
        String hashBefore = seeded.getPassword();

        long seededId = collaboratorRepository
                .findByUserUsername(SEEDED_COLLABORATOR_USERNAME)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/collaborators/{id}/credentials", seededId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", Matchers.containsString("No recoverable initial credentials")));

        // Nothing was regenerated, and the original credentials still authenticate.
        assertThat(userRepository.findByUsername(SEEDED_COLLABORATOR_USERNAME).orElseThrow().getPassword())
                .isEqualTo(hashBefore);
        login(SEEDED_COLLABORATOR_USERNAME, SEEDED_COLLABORATOR_PASSWORD);
    }

    @Test
    void unknownCollaborator_Returns404() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/collaborators/{id}/credentials", 987654321L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void encryptedValueIsStored_NotPlaintext() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        var created = objectMapper.readTree(createCollaborator(token));
        String matricule = created.get("matricule").asText();
        String password = created.at("/initialCredentials/temporaryPassword").asText();

        User stored = userRepository.findByUsername(matricule).orElseThrow();

        assertThat(stored.getPassword()).startsWith("$2").isNotEqualTo(password);
        assertThat(stored.getEncryptedInitialPassword())
                .isNotNull()
                .isNotEqualTo(password)
                .doesNotContain(password);
        assertThat(passwordEncoder.matches(password, stored.getPassword())).isTrue();
    }

    @Test
    void existingAdminAndSeededCollaboratorLoginsStillWork() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(passwordEncoder.matches(ADMIN_PASSWORD,
                userRepository.findByUsername(ADMIN_USERNAME).orElseThrow().getPassword())).isTrue();

        String collaboratorToken = login(SEEDED_COLLABORATOR_USERNAME, SEEDED_COLLABORATOR_PASSWORD);
        mockMvc.perform(get("/api/collaborators/me")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk());
    }

    private String createCollaborator(String token) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Rachid%s","lastName":"Bennani%s","profile":"DEV"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
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