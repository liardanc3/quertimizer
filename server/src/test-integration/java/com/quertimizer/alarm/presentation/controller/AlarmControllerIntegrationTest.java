package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.application.port.UserAlarmRepository;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.alarm.domain.model.AdminDirectAlarm;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AlarmController")
class AlarmControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAlarmRepository userAlarmRepository;

    @Nested
    @DisplayName("GET /alarms")
    class GetAlarms {

        @Test
        @DisplayName("성공 (알람 목록 조회)")
        void successWhenAuthenticated() throws Exception {
            // given
            String handle = "beginner02";
            userAlarmRepository.save(UserAlarm.create(new AdminDirectAlarm(handle, "목록 조회 알림"), "{}"));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner02@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/alarms").with(user));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.alarms").isArray())
                    .andExpect(jsonPath("$.unreadCount").isNumber());
        }

        @Test
        @DisplayName("실패 (미인증)")
        void unauthorizedWhenAuthenticationMissing() throws Exception {
            // given

            // when
            var result = mockMvc.perform(get("/alarms"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /alarms/read-all")
    class MarkAllRead {

        @Test
        @DisplayName("성공 (전체 읽음)")
        void successWhenUnreadAlarmsExist() throws Exception {
            // given
            String handle = "beginner03";
            userAlarmRepository.save(UserAlarm.create(new AdminDirectAlarm(handle, "전체 읽음 알림"), "{}"));
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner03@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/alarms/read-all").with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(userAlarmRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(handle)).isEmpty();
        }

        @Test
        @DisplayName("실패 (미인증)")
        void unauthorizedWhenAuthenticationMissing() throws Exception {
            // given

            // when
            var result = mockMvc.perform(post("/alarms/read-all"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /alarms/{alarmId}/read")
    class MarkRead {

        @Test
        @DisplayName("성공 (단일 읽음)")
        void successWhenAlarmExists() throws Exception {
            // given
            String handle = "beginner04";
            UserAlarm alarm = userAlarmRepository.save(UserAlarm.create(new AdminDirectAlarm(handle, "단일 읽음 알림"), "{}"));
            long alarmId = alarm.getAlarmId();
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner04@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/alarms/{alarmId}/read", alarmId).with(user));

            // then
            result.andExpect(status().isNoContent());
            assertThat(userAlarmRepository.findByAlarmIdAndHandle(alarmId, handle).orElseThrow().isRead()).isTrue();
        }

        @Test
        @DisplayName("실패 (대상 없음)")
        void notFoundWhenAlarmMissing() throws Exception {
            // given
            long alarmId = 99999999L;
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user =
                    user("beginner04@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(post("/alarms/{alarmId}/read", alarmId).with(user));

            // then
            result.andExpect(status().isNotFound());
        }
    }
}
