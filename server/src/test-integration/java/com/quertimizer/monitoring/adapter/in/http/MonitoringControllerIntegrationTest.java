package com.quertimizer.monitoring.adapter.in.http;

import com.quertimizer.judge.application.input.DatabaseNodeConfigUpdateInput;
import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;
import com.quertimizer.judge.application.port.in.UpdateDatabaseNodeConfigUseCase;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.monitoring.application.output.DatabaseStatusOutput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;
import com.quertimizer.monitoring.application.output.SystemResourceOutput;
import com.quertimizer.monitoring.application.port.in.GetDatabaseStatusUseCase;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.monitoring.domain.model.MonitoringLogLevel;
import com.quertimizer.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MonitoringController")
class MonitoringControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetSystemResourcesUseCase getSystemResources;
    @MockitoBean private GetDatabaseStatusUseCase getDatabaseStatus;
    @MockitoBean private GetServerLogsUseCase getServerLogs;
    @MockitoBean private UpdateDatabaseNodeConfigUseCase updateDatabaseNodeConfig;

    @Nested
    @DisplayName("GET /admin/monitoring/resources")
    class GetSystemResources {

        @Test
        @DisplayName("성공 (관리자 서버 리소스 조회)")
        void successWhenAdminRequestsResources() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin = admin();
            when(getSystemResources.execute())
                    .thenReturn(new SystemResourceOutput(12.5, 3.1, 0.7, 1000L, 400L, 2000L, 500L, 60L));

            // when
            var result = mockMvc.perform(get("/admin/monitoring/resources").with(admin));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.systemCpuUsagePercent").value(12.5))
                    .andExpect(jsonPath("$.processCpuUsagePercent").value(3.1))
                    .andExpect(jsonPath("$.usedMemoryBytes").value(400));
        }

        @Test
        @DisplayName("실패 (일반 사용자 접근)")
        void failWhenUserRequestsResources() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor user = user("user@example.com").roles(UserRole.USER.name());

            // when
            var result = mockMvc.perform(get("/admin/monitoring/resources").with(user));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /admin/monitoring/database-status")
    class GetDatabaseStatus {

        @Test
        @DisplayName("성공 (관리자 DB 상태 조회)")
        void successWhenAdminRequestsDatabaseStatus() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin = admin();
            when(getDatabaseStatus.execute())
                    .thenReturn(new DatabaseStatusOutput(2, 3, List.of(), List.of(), List.of(), List.of()));

            // when
            var result = mockMvc.perform(get("/admin/monitoring/database-status").with(admin));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalWaitingCount").value(2))
                    .andExpect(jsonPath("$.totalRunningCount").value(3))
                    .andExpect(jsonPath("$.queues").isArray());
        }
    }

    @Nested
    @DisplayName("GET /admin/monitoring/logs")
    class GetServerLogs {

        @Test
        @DisplayName("성공 (관리자 서버 로그 조회)")
        void successWhenAdminRequestsLogs() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin = admin();
            when(getServerLogs.execute(any()))
                    .thenReturn(new ServerLogOutput(MonitoringLogLevel.INFO, LocalDate.of(2026, 5, 13), true, List.of("line1", "line2")));

            // when
            var result = mockMvc.perform(get("/admin/monitoring/logs")
                    .param("level", "info")
                    .param("date", "2026-05-13")
                    .param("size", "20")
                    .with(admin));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.level").value("info"))
                    .andExpect(jsonPath("$.date").value("2026-05-13"))
                    .andExpect(jsonPath("$.lines[0]").value("line1"));
        }
    }

    @Nested
    @DisplayName("PUT /admin/monitoring/database-node-configs/{databaseId}")
    class UpdateDatabaseNodeConfig {

        @Test
        @DisplayName("성공 (관리자 DB 노드 설정 변경)")
        void successWhenAdminUpdatesDatabaseNodeConfig() throws Exception {
            // given
            SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin = admin();
            when(updateDatabaseNodeConfig.execute(any()))
                    .thenReturn(new DatabaseNodeConfigOutput("pg-worker-1", "PostgreSQL Worker 1", DbmsType.POSTGRESQL, true, 4,
                            LocalDateTime.of(2026, 5, 13, 12, 0)));

            // when
            var result = mockMvc.perform(put("/admin/monitoring/database-node-configs/{databaseId}", "pg-worker-1")
                    .with(admin)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"enabled":true,"maxConcurrency":4}
                            """));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.databaseId").value("pg-worker-1"))
                    .andExpect(jsonPath("$.dbmsType").value("postgresql"))
                    .andExpect(jsonPath("$.maxConcurrency").value(4));

            ArgumentCaptor<DatabaseNodeConfigUpdateInput> inputCaptor = ArgumentCaptor.forClass(DatabaseNodeConfigUpdateInput.class);
            verify(updateDatabaseNodeConfig).execute(inputCaptor.capture());
            assertThat(inputCaptor.getValue().getDatabaseId()).isEqualTo("pg-worker-1");
            assertThat(inputCaptor.getValue().getMaxConcurrency()).isEqualTo(4);
        }
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("admin@example.com").roles(UserRole.ADMIN.name());
    }
}
