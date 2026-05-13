package com.quertimizer.alarm.adapter.in.http;

import com.quertimizer.alarm.application.port.out.AlarmTemplateRepositoryPort;
import com.quertimizer.alarm.domain.model.AlarmType;
import com.quertimizer.user.domain.model.UserRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AlarmTemplateController")
class AlarmTemplateControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AlarmTemplateRepositoryPort alarmTemplateRepository;

    @Nested
    @DisplayName("GET /admin/alarm-templates")
    class GetAdminAlarmTemplates {

        @Test
        @DisplayName("성공 (템플릿 목록)")
        void successWhenAdminAuthenticated() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(get("/admin/alarm-templates").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("실패 (권한 없음)")
        void forbiddenWhenUserRole() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner01@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/alarm-templates").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /admin/alarm-templates/{alarmType}")
    class UpdateAlarmTemplate {

        @Test
        @DisplayName("성공 (템플릿 수정)")
        void successWhenTemplateExists() throws Exception {
            // given
            String alarmType = AlarmType.LIKE_MY_POST.getValue();
            String requestBody = "{\"sentence\":\"수정 문장\",\"description\":\"수정 설명\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/alarm-templates/{alarmType}", alarmType).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.sentence").value("수정 문장"));
            assertThat(alarmTemplateRepository.findById(AlarmType.LIKE_MY_POST.getValue()).orElseThrow().getDescription()).isEqualTo("수정 설명");
        }

        @Test
        @DisplayName("실패 (요청값 오류)")
        void badRequestWhenSentenceBlank() throws Exception {
            // given
            String alarmType = AlarmType.LIKE_MY_POST.getValue();
            String requestBody = "{\"sentence\":\"\",\"description\":\"수정 설명\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/alarm-templates/{alarmType}", alarmType).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenTemplateMissing() throws Exception {
            // given
            String alarmType = "UNKNOWN";
            String requestBody = "{\"sentence\":\"수정 문장\",\"description\":\"수정 설명\"}";
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("admin@example.com").roles(UserRole.ADMIN.name());

            // when
            var result = mockMvc.perform(put("/admin/alarm-templates/{alarmType}", alarmType).with(csrf())
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            result.andExpect(status().isNotFound());
        }
    }
}
