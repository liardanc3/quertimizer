package com.quertimizer.alarm.adapter.in.web;

import com.quertimizer.alarm.application.port.out.UserAlarmRepositoryPort;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.global.constant.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminAlarmController")
class AdminAlarmControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAlarmRepositoryPort userAlarmRepository;

    @Nested
    @DisplayName("GET /admin/alarms/recipients")
    class SearchAlarmRecipients {

        @Test
        @DisplayName("성공 (수신자 검색)")
        void successWhenKeywordMatches() throws Exception {
            // given
            String keyword = "beginner01";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/alarms/recipients")
                    .with(user)
                    .param("keyword", keyword));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].handle").value("beginner01"));
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            String keyword = "beginner01";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/alarms/recipients")
                    .with(user)
                    .param("keyword", keyword));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /admin/alarms/send")
    class SendAdminAlarm {

        @Test
        @DisplayName("성공 (알람 저장)")
        void successWhenRecipientExists() throws Exception {
            // given
            String requestBody = "{\"recipientHandles\":[\"beginner01\"],\"message\":\"테스트 알림\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/alarms/send").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.sentCount").value(1));
            assertThat(userAlarmRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc("beginner01"))
                    .extracting(UserAlarm::getMessage)
                    .contains("테스트 알림");
        }

        @Test
        @DisplayName("실패 (수신자 없음)")
        void badRequestWhenRecipientMissing() throws Exception {
            // given
            String requestBody = "{\"recipientHandles\":[\"missing-handle\"],\"message\":\"테스트 알림\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/alarms/send").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenMessageBlank() throws Exception {
            // given
            String requestBody = "{\"recipientHandles\":[\"beginner01\"],\"message\":\"\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(post("/admin/alarms/send").with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }
    }
}
