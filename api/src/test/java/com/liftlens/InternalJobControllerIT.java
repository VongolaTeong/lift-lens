package com.liftlens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** The external-cron entry point must reject callers without the shared secret (CLAUDE.md §6). */
@AutoConfigureMockMvc
class InternalJobControllerIT extends AbstractPostgresIT {

    private static final String TOKEN = "dev-internal-token"; // application.yml default

    @Autowired
    MockMvc mockMvc;

    @Test
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(post("/internal/jobs/detect-insights"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongToken() throws Exception {
        mockMvc.perform(post("/internal/jobs/detect-insights").header("X-Internal-Token", "nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runsJobWithValidToken() throws Exception {
        mockMvc.perform(post("/internal/jobs/detect-insights").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void unknownJobReturns404() throws Exception {
        mockMvc.perform(post("/internal/jobs/does-not-exist").header("X-Internal-Token", TOKEN))
                .andExpect(status().isNotFound());
    }
}
