package com.quertimizer.dashboard.adapter.in.web;

import com.quertimizer.global.constant.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DashboardController")
class DashboardControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /dashboard")
    class GetDashboard {

        @Test
        @DisplayName("성공 (미인증 대시보드)")
        void successWhenUnauthenticated() throws Exception {
            // given

            // when
            var result = mockMvc.perform(get("/dashboard"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false))
                    .andExpect(jsonPath("$.communityPosts").isArray())
                    .andExpect(jsonPath("$.problems").isArray());
        }

        @Test
        @DisplayName("성공 (인증 대시보드)")
        void successWhenAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/dashboard").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.currentHandle").value("beginner01"));
        }
    }
}
