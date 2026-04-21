package com.quertimizer.endpoint.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.websocket.dto.AlarmSocketRes;
import com.quertimizer.endpoint.websocket.dto.ProblemExecuteRes;
import com.quertimizer.endpoint.websocket.dto.ProblemSubmitProgressRes;
import com.quertimizer.endpoint.websocket.dto.ProblemSocketReq;
import com.quertimizer.log.LogFormatter;
import com.quertimizer.service.ProblemQueryService;
import com.quertimizer.service.ProblemWorkspaceService;
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
        String actor = resolveActor(session);
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = (String) session.getAttributes().get("sessionId");

        // 같은 HttpSession에서 열린 WebSocket 연결 추적
        if (sessionId != null && !sessionId.isBlank()) {
            sessionSockets.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }

        if (userId != null && !userId.isBlank()) {
            userSockets.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }

        // 연결 완료 로그 기록 및 초기 연결 메시지 전송
        log.info("{}", logFormatter.formatWebSocketLine(actor, "WebSocket connection open", null));
        sendTextMessage(session, "{\"type\":\"connected\",\"userId\":\"" + userId + "\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
        String actor = resolveActor(session);
        String sessionId = (String) session.getAttributes().get("sessionId");
        String userId = (String) session.getAttributes().get("userId");

        if (sessionId != null && !sessionId.isBlank()) {
            removeSessionSocket(sessionId, session);
        }

        if (userId != null && !userId.isBlank()) {
            removeUserSocket(userId, session);
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

    public void sendAlarm(String userId, AlarmSocketRes payload) throws Exception {
        Set<WebSocketSession> userWebSocketSessions = userSockets.get(userId);
        if (userWebSocketSessions == null || userWebSocketSessions.isEmpty()) {
            return;
        }

        for (WebSocketSession userWebSocketSession : Set.copyOf(userWebSocketSessions)) {
            if (!userWebSocketSession.isOpen()) {
                removeUserSocket(userId, userWebSocketSession);
                continue;
            }

            try {
                sendObjectMessage(userWebSocketSession, payload);
            } catch (Exception exception) {
                log.warn("알람 WebSocket 전송에 실패했다.", exception);
            }
        }
    }

    private void handleProblemExecute(WebSocketSession session, ProblemSocketReq request) throws Exception {
        String authenticatedUserId = resolveAuthenticatedUserId(session);

        // SQL 실행은 별도 쓰레드에서 비동기 처리
        problemExecutingExecutor.execute(() -> executeProblemQuery(session, request, authenticatedUserId));
    }

    private void handleProblemExecutePage(WebSocketSession session, ProblemSocketReq request) throws Exception {
        String authenticatedUserId = resolveAuthenticatedUserId(session);
        problemExecutingExecutor.execute(() -> executeProblemQueryPage(session, request, authenticatedUserId));
    }

    private void handleProblemSubmit(WebSocketSession session, ProblemSocketReq request) throws Exception {
        String authenticatedUserId = resolveAuthenticatedUserId(session);

        // SQL 제출도 동일한 실행 쓰레드에서 비동기 처리
        problemExecutingExecutor.execute(() -> submitProblemQuery(session, request, authenticatedUserId));
    }

    private void handleProblemExecuteStop(WebSocketSession session, ProblemSocketReq request) {
        problemQueryService.cancelInteractiveExecution(session.getId());
    }

    private void handleProblemLeave(WebSocketSession session, ProblemSocketReq request) throws Exception {

        // 명시적 페이지 이탈 후 작업용 스키마 정리
        problemWorkspaceService.handleExplicitLeave(session.getId());
        sendObjectMessage(session, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    private void sendObjectMessage(WebSocketSession session, Object payload) throws Exception {
        sendTextMessage(session, objectMapper.writeValueAsString(payload));
    }

    private void sendTextMessage(WebSocketSession session, String payload) throws Exception {
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

    private String resolveAuthenticatedUserId(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 없다.");
        }

        return userId;
    }

    private DbmsType resolveDbmsType(String dbms) {
        return "oracle".equalsIgnoreCase(dbms) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private String resolveActor(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");

        if (userId != null && !userId.isBlank()) {
            return userId;
        }

        if (session.getRemoteAddress() != null && session.getRemoteAddress().getAddress() != null) {
            return session.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private void removeSessionSocket(String sessionId, WebSocketSession session) {
        sessionSockets.computeIfPresent(sessionId, (key, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                return null;
            }

            return sessions;
        });
    }

    private void removeUserSocket(String userId, WebSocketSession session) {
        userSockets.computeIfPresent(userId, (key, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                return null;
            }

            return sessions;
        });
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return "문제 실행 처리에 실패했다.";
    }

    private void logLines(List<String> logLines) {
        for (String logLine : logLines) {
            log.info("{}", logLine);
        }
    }

    private void executeProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedUserId) {
        try {
            ProblemQueryService.QueryExecutionResult executionResult = problemQueryService.executeInteractiveSql(
                    authenticatedUserId,
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
                log.warn("문제 실행 실패 응답 전송에 실패했다.", sendException);
            }
        }
    }

    private void executeProblemQueryPage(WebSocketSession session, ProblemSocketReq request, String authenticatedUserId) {
        try {
            ProblemQueryService.QueryExecutionResult executionResult = problemQueryService.executeInteractiveSql(
                    authenticatedUserId,
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
                log.warn("문제 실행 페이지 응답 전송에 실패했다.", sendException);
            }
        }
    }

    private void submitProblemQuery(WebSocketSession session, ProblemSocketReq request, String authenticatedUserId) {
        try {
            ProblemQueryService.ProblemSubmitResult submitResult = problemQueryService.submitProblemSql(
                    authenticatedUserId,
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
                log.warn("문제 제출 실패 응답 전송에 실패했다.", sendException);
            }
        }
    }

    private void sendProblemSubmitProgressMessage(WebSocketSession session, ProblemQueryService.ProblemSubmitProgress progress) {
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
            log.warn("제출 진행 상태 응답 전송에 실패했다.", exception);
        }
    }

}
