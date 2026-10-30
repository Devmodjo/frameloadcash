package com.frameload.cash.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.UserRequest;
import com.frameload.cash.dto.UserResponse;
import com.frameload.cash.enums.Role;
import com.frameload.cash.services.UserServices;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste le contrat HTTP des utilisateurs : codes de statut, messages d'erreur et absence de tout mot de passe
 * ou hachage dans les réponses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserServices userServices;

    private String uniqueUsername() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String json(String username, String password, String role) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":" + role + "}";
    }

    @Test
    void createUser_returns201WithoutAnyPasswordOrHash() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueUsername(), "Secret-12345", "\"AGENT\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("$2"))));
    }

    @Test
    void createUser_withInvalidData_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueUsername(), "court", "\"AGENT\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("mot de passe")));
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json("a b", "Secret-12345", "\"AGENT\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueUsername(), "Secret-12345", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createUser_withAnUnknownRole_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueUsername(), "Secret-12345", "\"SUPER_ADMIN\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void createUser_withADuplicateUsername_returns409() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, "Secret-12345", "\"AGENT\"")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, "Secret-12345", "\"ADMIN\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USERNAME"));
    }

    @Test
    void listUsers_neverExposesPasswordsOrHashes() throws Exception {
        UserResponse created = userServices.create(new UserRequest(uniqueUsername(), "Secret-12345", Role.SUPERVISOR));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + created.id() + ")].role").value("SUPERVISOR"))
                .andExpect(jsonPath("$[*].password").doesNotExist())
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("$2a$"))));
    }

    @Test
    void changeStatus_deactivatesAUser() throws Exception {
        UserResponse created = userServices.create(new UserRequest(uniqueUsername(), "Secret-12345", Role.AGENT));

        mockMvc.perform(patch("/api/v1/users/" + created.id() + "/status").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void changeStatus_withAnUnknownStatusOrUser_returns400Or404() throws Exception {
        UserResponse created = userServices.create(new UserRequest(uniqueUsername(), "Secret-12345", Role.AGENT));

        mockMvc.perform(patch("/api/v1/users/" + created.id() + "/status").param("status", "SUSPENDU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        mockMvc.perform(patch("/api/v1/users/999999/status").param("status", "INACTIVE"))
                .andExpect(status().isNotFound());
    }
}