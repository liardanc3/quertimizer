package com.quertimizer.problem.adapter.in.websocket;

import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemExecuteRes;
import com.quertimizer.problem.adapter.in.websocket.dto.ProblemSocketReq;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.application.port.in.CancelProblemExecutionUseCase;
import com.quertimizer.problem.application.port.in.CloseProblemExecutionSessionUseCase;
import com.quertimizer.problem.application.port.in.ExecuteProblemSqlUseCase;
import com.quertimizer.problem.application.port.in.SubmitProblemSqlUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProblemSocketMessageHandler")
class ProblemSocketMessageHandlerIntegrationTest {

    @Autowired private ProblemSocketMessageHandler problemSocketMessageHandler;

    @MockitoBean private ExecuteProblemSqlUseCase executeProblemSql;
    @MockitoBean private SubmitProblemSqlUseCase submitProblemSql;
    @MockitoBean private CancelProblemExecutionUseCase cancelProblemExecution;
    @MockitoBean private CloseProblemExecutionSessionUseCase closeProblemExecutionSession;
    @MockitoBean private WebSocketSender webSocketSender;

    @Nested
    @DisplayName("MESSAGE problem.execute")
    class HandleProblemExecute {

        @Test
        @DisplayName("성공 (SQL 실행 응답 전송)")
        void successWhenExecuteRequested() {
            // given
            ProblemSocketReq request = request();
            when(executeProblemSql.execute(any()))
                    .thenReturn(new ProblemExecutionOutput(
                            "P00001-00001", "SELECT", "OK",
                            List.of("email"), List.of(List.of("a@quertimizer.com")), List.of(),
                            1L, 1, 20, 5L, 1.2
                    ));

            // when
            problemSocketMessageHandler.handleProblemExecute(request, headerAccessor());

            // then
            ArgumentCaptor<ProblemExecutionInput> inputCaptor = ArgumentCaptor.forClass(ProblemExecutionInput.class);
            verify(executeProblemSql).execute(inputCaptor.capture());
            assertThat(inputCaptor.getValue().getHandle()).isEqualTo("solver");
            assertThat(inputCaptor.getValue().getProblemId()).isEqualTo("P00001-00001");

            verify(webSocketSender).sendToSessionUnchecked(
                    eq("solver"), eq("ws-1"),
                    argThat(payload -> payload instanceof ProblemExecuteRes res
                            && "problem.execute.result".equals(res.getType())
                            && res.isSuccess())
            );
        }
    }

    @Nested
    @DisplayName("MESSAGE problem.submit")
    class HandleProblemSubmit {

        @Test
        @DisplayName("성공 (SQL 제출 응답 전송)")
        void successWhenSubmitRequested() {
            // given
            ProblemSocketReq request = request();
            when(submitProblemSql.execute(any()))
                    .thenReturn(new ProblemSubmissionOutput("P00001-00001", true, "정답", 10L));

            // when
            problemSocketMessageHandler.handleProblemSubmit(request, headerAccessor());

            // then
            ArgumentCaptor<ProblemSubmissionInput> inputCaptor = ArgumentCaptor.forClass(ProblemSubmissionInput.class);
            verify(submitProblemSql).execute(inputCaptor.capture());
            assertThat(inputCaptor.getValue().getHandle()).isEqualTo("solver");
            assertThat(inputCaptor.getValue().getProblemId()).isEqualTo("P00001-00001");

            verify(webSocketSender).sendToSessionUnchecked(
                    eq("solver"), eq("ws-1"),
                    argThat(payload -> payload instanceof ProblemExecuteRes res
                            && "problem.submit.result".equals(res.getType())
                            && res.isSuccess())
            );
        }
    }

    @Nested
    @DisplayName("MESSAGE problem.execute.stop")
    class HandleProblemExecuteStop {

        @Test
        @DisplayName("성공 (실행 취소)")
        void successWhenStopRequested() {
            // given
            ProblemSocketReq request = request();

            // when
            problemSocketMessageHandler.handleProblemExecuteStop(request, headerAccessor());

            // then
            verify(cancelProblemExecution).execute("problem-execution-session:ws-1");
        }
    }

    @Nested
    @DisplayName("MESSAGE problem.leave")
    class HandleProblemLeave {

        @Test
        @DisplayName("성공 (실행 세션 정리)")
        void successWhenLeaveRequested() {
            // given
            ProblemSocketReq request = request();

            // when
            problemSocketMessageHandler.handleProblemLeave(request, headerAccessor());

            // then
            verify(closeProblemExecutionSession).execute("problem-execution-session:ws-1");
            verify(webSocketSender).sendToSessionUnchecked(
                    eq("solver"), eq("ws-1"),
                    argThat(payload -> payload instanceof ProblemExecuteRes res
                            && "problem.leave.result".equals(res.getType())
                            && res.isSuccess())
            );
        }
    }

    @Nested
    @DisplayName("SessionDisconnectEvent")
    class HandleSessionDisconnect {

        @Test
        @DisplayName("성공 (연결 종료 세션 정리)")
        void successWhenSessionDisconnected() {
            // given
            SessionDisconnectEvent event = new SessionDisconnectEvent(
                    this, MessageBuilder.withPayload(new byte[0]).build(), "ws-1", CloseStatus.NORMAL
            );

            // when
            problemSocketMessageHandler.handleSessionDisconnect(event);

            // then
            verify(closeProblemExecutionSession).execute("problem-execution-session:ws-1");
        }
    }

    private static ProblemSocketReq request() {
        return new ProblemSocketReq(
                "P00001-00001", "SELECT email FROM customers", "postgresql",
                1, 20, List.of("CREATE INDEX idx_customers_email ON customers(email)")
        );
    }

    private static SimpMessageHeaderAccessor headerAccessor() {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId("ws-1");
        headerAccessor.setSessionAttributes(new HashMap<>(Map.of("handle", "solver")));
        return headerAccessor;
    }
}
