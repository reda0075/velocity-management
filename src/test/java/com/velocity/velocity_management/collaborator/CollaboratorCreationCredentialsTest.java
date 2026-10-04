package com.velocity.velocity_management.collaborator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.velocity_management.auth.dto.AuthRequest;
import com.velocity.velocity_management.auth.entity.User;
import com.velocity.velocity_management.auth.enums.Role;
import com.velocity.velocity_management.auth.repository.UserRepository;
import com.velocity.velocity_management.collaborator.repository.CollaboratorRepository;
import com.velocity.velocity_management.collaborator.service.InitialCredentialsGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class CollaboratorCreationCredentialsTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

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

    @Autowired
    private InitialCredentialsGenerator initialCredentialsGenerator;

    @Test
    void matriculeGeneration_IsUnchanged_AndCredentialsNeverAlterIt() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // Base format is untouched: firstInitial.LASTNAME_PROFILE.
        String first = createCollaborator(token, "Zacharie", "Nkemi", "DEV");
        assertThat(first).isEqualTo("Z.NKEMI_DEV");
        // Only the pre-existing collision counter may append digits.
        assertThat(createCollaborator(token, "Zacharie", "Nkemi", "DEV")).isEqualTo("Z.NKEMI_DEV01");

        // DEVOPS uses the "DO" profile code, QA uses "QA" — unchanged by this feature.
        assertThat(createCollaborator(token, "Quentin", "Alba", "QA")).isEqualTo("Q.ALBA_QA");
        assertThat(createCollaborator(token, "Umberto", "Rocha", "DEVOPS")).isEqualTo("U.ROCHA_DO");

        // The seeded profile keeps the matricule it already had; creation moved to the counter form.
        assertThat(collaboratorRepository.findByMatricule("T.COLLABORATOR_DEV")).isPresent();

        // Every username is the matricule verbatim: no "_NNNN" suffix on the login itself.
        for (String matricule : List.of(first, "Z.NKEMI_DEV01", "Q.ALBA_QA", "U.ROCHA_DO")) {
            assertThat(userRepository.findByUsername(matricule)).isPresent();
            assertThat(userRepository.findByUsername(matricule + "_0000")).isEmpty();
        }
    }

    private String createCollaborator(String token, String firstName, String lastName, String profile)
            throws Exception {
        String body = mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s","profile":"%s"}
                                """.formatted(firstName, lastName, profile)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("matricule").asText();
    }

    @Test
    void createCollaborator_ReturnsUsernameEqualToMatriculeAndDerivedPassword() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String suffix = uniqueSuffix();

        String body = mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Alice%s","lastName":"Martin%s","profile":"DEV"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matricule").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Alice" + suffix))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(body);
        String matricule = created.get("matricule").asText();
        String username = created.at("/initialCredentials/username").asText();
        String password = created.at("/initialCredentials/temporaryPassword").asText();

        // Requirement 2: the username is exactly the generated matricule.
        assertThat(username).isEqualTo(matricule);

        // Requirements 3 and 4: <matricule> + "_" + exactly four numeric digits.
        assertThat(password).matches("^" + Pattern.quote(matricule) + "_[0-9]{4}$");

        String digits = password.substring(matricule.length() + 1);
        assertThat(digits).hasSize(4).containsOnlyDigits();

        // Requirement 1: the collaborator was actually persisted.
        assertThat(created.get("id").asLong()).isPositive();
        assertThat(created.get("active").asBoolean()).isTrue();
    }

    @Test
    void createCollaborator_PersistsBcryptHashAndNeverThePlaintextPassword() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String suffix = uniqueSuffix();

        String body = mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Bruno%s","lastName":"Petit%s","profile":"QA"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(body);
        String matricule = created.get("matricule").asText();
        String password = created.at("/initialCredentials/temporaryPassword").asText();

        // Requirements 5 and 6: only the BCrypt hash is stored.
        User storedUser = userRepository.findByUsername(matricule).orElseThrow();
        assertThat(storedUser.getPassword()).isNotEqualTo(password);
        assertThat(storedUser.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches(password, storedUser.getPassword())).isTrue();
    }

    @Test
    void createdCollaborator_CanAuthenticateWithMatriculeCredentials() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String suffix = uniqueSuffix();

        String body = mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Chloe%s","lastName":"Roux%s","profile":"DEVOPS"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(body);
        String matricule = created.get("matricule").asText();
        String password = created.at("/initialCredentials/temporaryPassword").asText();

        // Requirements 9 and 10: the issued credentials work and carry the COLLABORATOR role.
        String collaboratorToken = login(matricule, password);

        mockMvc.perform(get("/api/collaborators/me")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value(matricule))
                .andExpect(jsonPath("$.role").doesNotExist());

        // Requirement 10: the role is COLLABORATOR, and it is not an administrator.
        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void normalResponses_NeverExposeTheInitialCredentials() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String suffix = uniqueSuffix();

        String body = mockMvc.perform(post("/api/collaborators")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Diana%s","lastName":"Blanc%s","profile":"DEV"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(body);
        long id = created.get("id").asLong();

        // Requirement 8: credentials exist on the creation response only.
        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].initialCredentials").doesNotExist())
                .andExpect(jsonPath("$[*].temporaryPassword").doesNotExist());

        mockMvc.perform(get("/api/collaborators/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialCredentials").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(put("/api/collaborators/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Diana%s","lastName":"Blanc%s","profile":"QA","active":true}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialCredentials").doesNotExist());

        String collaboratorToken = login(
                created.get("matricule").asText(),
                created.at("/initialCredentials/temporaryPassword").asText());

        mockMvc.perform(get("/api/collaborators/me")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialCredentials").doesNotExist());
    }

    @Test
    void existingAdminCredentials_StillWorkAfterTheChange() throws Exception {
        // Requirement 11: the seeded administrator is untouched.
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/collaborators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User admin = userRepository.findByUsername(ADMIN_USERNAME).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())).isTrue();
    }

    @Test
    void generatedPasswords_AlwaysMatchTheRequiredFormatAndVaryBetweenCalls() {
        // The four digits are random, so repeated calls must not all produce the same suffix.
        Set<String> suffixes = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            var credentials = initialCredentialsGenerator.generate("T.COLLABORATOR_DEV01");

            assertThat(credentials.username()).isEqualTo("T.COLLABORATOR_DEV01");
            assertThat(credentials.temporaryPassword()).matches("^T\\.COLLABORATOR_DEV01_[0-9]{4}$");

            suffixes.add(credentials.temporaryPassword().substring("T.COLLABORATOR_DEV01_".length()));
        }

        assertThat(suffixes).hasSizeGreaterThan(1);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 6);
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
