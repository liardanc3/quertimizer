package com.quertimizer.monitoring.adapter.in.websocket;

import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringDatabaseStatusSocketRes;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringLogSocketReq;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringLogSocketRes;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringResourceSocketRes;
import com.quertimizer.monitoring.application.output.DatabaseStatusOutput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;
import com.quertimizer.monitoring.application.output.SystemResourceOutput;
import com.quertimizer.monitoring.application.port.in.GetDatabaseStatusUseCase;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.monitoring.domain.model.MonitoringLogLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MonitoringSocketMessageHandler")
class MonitoringSocketMessageHandlerIntegrationTest {

    @Autowired private MonitoringSocketMessageHandler monitoringSocketMessageHandler;

    @MockitoBean private GetSystemResourcesUseCase getSystemResources;
    @MockitoBean private GetDatabaseStatusUseCase getDatabaseStatus;
    @MockitoBean private GetServerLogsUseCase getServerLogs;
    @MockitoBean private WebSocketSender webSocketSender;

    @Nested
    @DisplayName("MESSAGE monitoring.resources")
    class HandleMonitoringResources {

        @Test
        @DisplayName("성공 (서버 리소스 응답 전송)")
        void successWhenResourcesRequested() throws Exception {
            // given
            when(getSystemResources.execute())
                    .thenReturn(new SystemResourceOutput(10.0, 2.0, 0.5, 1000L, 300L, 2000L, 700L, 30L));

            // when
            monitoringSocketMessageHandler.handleMonitoringResources(headerAccessor());

            // then
            verify(webSocketSender).sendToSession(
                    eq("admin"), eq("ws-1"),
                    argThat(payload -> payload instanceof MonitoringResourceSocketRes res
                            && "monitoring.resources.result".equals(res.getType())
                            && res.getResource().getUsedMemoryBytes() == 300L)
            );
        }
    }

    @Nested
    @DisplayName("MESSAGE monitoring.database-status")
    class HandleMonitoringDatabaseStatus {

        @Test
        @DisplayName("성공 (DB 상태 응답 전송)")
        void successWhenDatabaseStatusRequested() throws Exception {
            // given
            when(getDatabaseStatus.execute())
                    .thenReturn(new DatabaseStatusOutput(1, 2, List.of(), List.of(), List.of(), List.of()));

            // when
            monitoringSocketMessageHandler.handleMonitoringDatabaseStatus(headerAccessor());

            // then
            verify(webSocketSender).sendToSession(
                    eq("admin"), eq("ws-1"),
                    argThat(payload -> payload instanceof MonitoringDatabaseStatusSocketRes res
                            && "monitoring.database-status.result".equals(res.getType())
                            && res.getStatus().getTotalWaitingCount() == 1)
            );
        }
    }

    @Nested
    @DisplayName("MESSAGE monitoring.logs.subscribe")
    class HandleMonitoringLogsSubscribe {

        @Test
        @DisplayName("성공 (로그 스냅샷 응답 전송)")
        void successWhenLogsSubscribed() {
            // given
            MonitoringLogSocketReq request = new MonitoringLogSocketReq();
            request.setLevel("info");
            request.setDate("2026-05-13");
            request.setSize(10);
            when(getServerLogs.execute(any()))
                    .thenReturn(new ServerLogOutput(MonitoringLogLevel.INFO, LocalDate.of(2026, 5, 13), true, List.of("line1")));

            // when
            monitoringSocketMessageHandler.handleMonitoringLogsSubscribe(request, headerAccessor());
            monitoringSocketMessageHandler.handleMonitoringLogsUnsubscribe(headerAccessor());

            // then
            verify(webSocketSender).sendToSessionSilently(
                    eq("admin"), eq("ws-1"),
                    argThat(payload -> payload instanceof MonitoringLogSocketRes res
                            && "monitoring.logs.snapshot".equals(res.getType())
                            && res.getLog().getLines().contains("line1"))
            );
        }
    }

    private static SimpMessageHeaderAccessor headerAccessor() {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId("ws-1");
        headerAccessor.setSessionAttributes(new HashMap<>(Map.of("handle", "admin")));
        return headerAccessor;
    }
}
