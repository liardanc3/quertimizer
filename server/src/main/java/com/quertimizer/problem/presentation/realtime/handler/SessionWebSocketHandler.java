package com.quertimizer.problem.presentation.realtime.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.alarm.presentation.realtime.dto.AlarmSocketRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemExecuteRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSubmitProgressRes;
import com.quertimizer.problem.presentation.realtime.dto.ProblemSocketReq;
import com.quertimizer.global.log.LogFormatter;
import com.quertimizer.problem.application.service.ProblemQueryService;
import com.quertimizer.problem.application.service.ProblemWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.auth.domain.model.AuthFailReason.LOGIN_INFORMATION_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.ALARM_SOCKET_SEND_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.EXECUTE_FAILURE_RESPONSE_SEND_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.EXECUTE_PAGE_RESPONSE_SEND_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.SUBMIT_FAILURE_RESPONSE_SEND_FAILED;
import static com.quertimizer.problem.domain.model.ProblemLogMessage.SUBMIT_PROGRESS_RESPONSE_SEND_FAILED;
import static com.quertimizer.problem.domain.model.ProblemQueryResultText.PROBLEM_EXECUTION_FAILED;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final ProblemQueryService problemQueryService;
    private final ProblemWorkspaceService problemWorkspaceService;
    @Qualifier("problemExecutingExecutor")
    private final TaskExecutor problemExecutingExecutor;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionSockets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSockets = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 주체와 세션 정보를 조회
        String actor = resolveActor(session);
        String handle = (String) session.getAttributes().get("handle");
        String sessionId = (String) session.getAttributes().get("sessionId");

        // 같은 HttpSession에서 열린 WebSocket 연결 추적
        if (sessionId != null && !sessionId.isBlank()) {
            sessionSockets.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }

        if (handle != null && !handle.isBlank()) {
            userSockets.computeIfAbsent(handle, key -> ConcurrentHashMap.newKeySet()).add(session);
        }

        // 연결 완료 로그 기록 및 초기 연결 메시지 전송
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection open", null));
        sendTextMessage(session, "{\"type\":\"connected\",\"handle\":\"" + handle + "\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 연결 주체와 로그 prefix를 조회
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        // 클라이언트에서 보낸 payload 로그 기록
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket client-send", null));
        logLines(logFormatter.formatRequestBodyLines(prefix, message.getPayload()));

        try {
            ProblemSocketReq request = objectMapper.readValue(message.getPayload(), ProblemSocketReq.class);
            if (request.type() == null || request.type().isBlank()) {
                return;
            }

            switch (request.type()) {
                case "problem.execute" -> handleProblemExecute(session, request);
                case "problem.execute.page" -> handleProblemExecutePage(session, request);
                case "problem.execute.stop" -> handleProblemExecuteStop(session, request);
                case "problem.submit" -> handleProblemSubmit(session, request);
                case "problem.leave" -> handleProblemLeave(session, request);
                default -> {
                }
            }
        } catch (Exception exception) {
            sendObjectMessage(session, ProblemExecuteRes.error(resolveErrorMessage(exception)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 연결 종료 세션 정리
        String actor = resolveActor(session);
        String sessionId = (String) session.getAttributes().get("sessionId");
        String handle = (String) session.getAttributes().get("handle");

        if (sessionId != null && !sessionId.isBlank()) {
            removeSessionSocket(sessionId, session);
        }

        if (handle != null && !handle.isBlank()) {
            removeUserSocket(handle, session);
        }

        // 연결 종료 후 문제 작업용 스키마 정리 예약
        problemWorkspaceService.handleConnectionClose(session.getId());
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection close", status.toString()));
    }

    public void closeSessionSockets(String sessionId) {

        // 로그아웃 후 HttpSession에 연결된 모든 WebSocket 종료
        Set<WebSocketSession> webSocketSessions = sessionSockets.remove(sessionId);
        if (webSocketSessions == null) {
            return;
        }

        for (WebSocketSession webSocketSession : webSocketSessions) {
            if (!webSocketSession.isOpen()) {
                continue;
            }

            try {
                webSocketSession.close(CloseStatus.NORMAL);
            } catch (IOException ignored) {
            }
        }
    }

    public void sendAlarm(String handle, AlarmSocketRes payload) throws Exception {
        // 사용자 알람 대상 세션 조회
        Set<WebSocketSession> userWebSocketSessions = userSockets.get(handle);
        if (userWebSocketSessions == null || userWebSocketSessions.isEmpty()) {
            return;
        }

        for (WebSocketSession userWebSocketSession : Set.copyOf(userWebSocketSessions)) {
            if (!userWebSocketSession.isOpen()) {
                removeUserSocket(handle, userWebSocketSession);
                continue;
            }

            try {
                sendObjectMessage(userWebSocketSession, payload);
            } catch (Exception exception) {
                log.warn(ALARM_SOCKET_SEND_FAILED.getMessage(), exception);
            }
        }
    }

    private void handleProblemExecute(WebSocketSession session, ProblemSocketReq request) throws Exception {
        // SQL 실행 주체를 확인 후 비동기 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> executeProblemQuery(session, request, authenticatedHandle));
    }

    private void handleProblemExecutePage(WebSocketSession session, ProblemSocketReq request) throws Exception {
        // SQL 실행 결과 페이지를 비동기 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> executeProblemQueryPage(session, request, authenticatedHandle));
    }

    private void handleProblemSubmit(WebSocketSession session, ProblemSocketReq request) throws Exception {
        // SQL 제출 주체를 확인 후 비동기 처리
        String authenticatedHandle = resolveAuthenticatedHandle(session);
        problemExecutingExecutor.execute(() -> submitProblemQuery(session, request, authenticatedHandle));
    }

    private void handleProblemExecuteStop(WebSocketSession session, ProblemSocketReq request) {
        // 진행 중 SQL 실행 중단
        problemQueryService.cancelInteractiveExecution(session.getId());
    }

    private void handleProblemLeave(WebSocketSession session, ProblemSocketReq request) throws Exception {
        // 명시적 페이지 이탈 후 작업용 스키마 정리
        problemWorkspaceService.handleExplicitLeave(session.getId());
        sendObjectMessage(session, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    private void sendObjectMessage(WebSocketSession session, Object payload) throws Exception {
        // 객체 payload를 텍스트 메시지로 변환 후 전송
        sendTextMessage(session, objectMapper.writeValueAsString(payload));
    }

    private void sendTextMessage(WebSocketSession session, String payload) throws Exception {
        // 전송 주체와 로그 prefix를 조회
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        // 서버에서 보내는 payload도 동일한 형식으로 로그 기록
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket server-send", null));
        logLines(logFormatter.formatResponseBodyLines(prefix, payload));

        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }

            session.sendMessage(new TextMessage(payload));
        }
    }

    private String resolveAuthenticatedHandle(WebSocketSession session) {
        // 인증 Handle 결정
        String handle = (String) session.getAttributes().get("handle");
        if (handle == null || handle.isBlank()) {
            throw new IllegalArgumentException(LOGIN_INFORMATION_NOT_FOUND.getMessage());
        }

        return handle;
    }

    private DbmsType resolveDbmsType(String dbms) {
        // DBMS 유형 결정
        return "oracle".equalsIgnoreCase(dbms) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private String resolveActor(WebSocketSession session) {
        // 주체 결정
        String handle = (String) session.getAttributes().get("handle");

        if (handle != null && !handle.isBlank()) {
            return handle;
        }

        if (session.getRemoteAddress() != null && session.getRemoteAddress().getAddress() != null) {
            return session.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private void removeSessionSocket(String sessionId, WebSocketSession session) {
        // 세션 소켓 제거
        sessionSockets.computeIfPresent(sessionId, (key, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                return null;
            }

            return sessions;
        });
    }

    private void removeUserSocket(String handle, WebSocketSession session) {
        // 사용자 소켓 제거
        userSockets.computeIfPresent(handle, (key, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                return null;
            }

            return sessions;
        });
    }

    private String resolveErrorMessage(Exception exception) {
        // Error 메시지 결정
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return PROBLEM_EXECUTION_FAILED.getText();
    }

    private void logLines(List<String> logLines) {
        // 로그 라인 생성
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }

    private void executeProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // execute 문제 쿼리 처리
        try {
            ProblemQueryService.QueryExecutionResult executionResult = problemQueryService.executeInteractiveSql(
                    authenticatedHandle,
                    session.getId(),
                    request.problemId(),
                    request.sql(),
                    resolveDbmsType(request.dbms()),
                    request.page(),
                    request.pageSize()
            );

            sendObjectMessage(session, ProblemExecuteRes.executionSuccess(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.currentPage(),
                    executionResult.pageSize(),
                    executionResult.executionTimeMs(),
                    executionResult.cost()
            ));
        } catch (Exception exception) {
            try {
                sendObjectMessage(session, ProblemExecuteRes.error(resolveErrorMessage(exception)));
            } catch (Exception sendException) {
                log.warn(EXECUTE_FAILURE_RESPONSE_SEND_FAILED.getMessage(), sendException);
            }
        }
    }

    private void executeProblemQueryPage(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // execute 문제 쿼리 페이지 처리
        try {
            ProblemQueryService.QueryExecutionResult executionResult = problemQueryService.executeInteractiveSql(
                    authenticatedHandle,
                    session.getId(),
                    request.problemId(),
                    request.sql(),
                    resolveDbmsType(request.dbms()),
                    request.page(),
                    request.pageSize()
            );

            sendObjectMessage(session, ProblemExecuteRes.executionSuccess(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.currentPage(),
                    executionResult.pageSize(),
                    executionResult.executionTimeMs(),
                    executionResult.cost()
            ));
        } catch (Exception exception) {
            try {
                sendObjectMessage(session, ProblemExecuteRes.error(resolveErrorMessage(exception)));
            } catch (Exception sendException) {
                log.warn(EXECUTE_PAGE_RESPONSE_SEND_FAILED.getMessage(), sendException);
            }
        }
    }

    private void submitProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedHandle) {
        // submit 문제 쿼리 처리
        try {
            ProblemQueryService.ProblemSubmitResult submitResult = problemQueryService.submitProblemSql(
                    authenticatedHandle,
                    session.getId(),
                    request.problemId(),
                    request.sql(),
                    resolveDbmsType(request.dbms()),
                    progress -> sendProblemSubmitProgressMessage(session, progress)
            );

            sendObjectMessage(session, submitResult.success()
                    ? ProblemExecuteRes.submitSuccess(
                    submitResult.problemId(),
                    submitResult.message(),
                    submitResult.executionTimeMs()
            )
                    : ProblemExecuteRes.submitFailure(
                    submitResult.problemId(),
                    submitResult.message()
            ));
        } catch (Exception exception) {
            try {
                sendObjectMessage(session, ProblemExecuteRes.submitFailure(request.problemId(), resolveErrorMessage(exception)));
            } catch (Exception sendException) {
                log.warn(SUBMIT_FAILURE_RESPONSE_SEND_FAILED.getMessage(), sendException);
            }
        }
    }

    private void sendProblemSubmitProgressMessage(WebSocketSession session, ProblemQueryService.ProblemSubmitProgress progress) {
        // 문제 제출 Progress 메시지 전송
        try {
            sendObjectMessage(session, ProblemSubmitProgressRes.of(
                    progress.problemId(),
                    progress.stepKey(),
                    progress.status(),
                    progress.message(),
                    progress.detailLines(),
                    progress.statementKey(),
                    progress.statementIndex(),
                    progress.statementSql(),
                    progress.statementMode(),
                    progress.statementReference()
            ));
        } catch (Exception exception) {
            log.warn(SUBMIT_PROGRESS_RESPONSE_SEND_FAILED.getMessage(), exception);
        }
    }

}
