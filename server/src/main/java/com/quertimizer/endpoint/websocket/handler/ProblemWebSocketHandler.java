package com.quertimizer.endpoint.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.websocket.dto.ProblemExecuteRes;
import com.quertimizer.endpoint.websocket.dto.ProblemSocketReq;
import com.quertimizer.log.LogFormatter;
import com.quertimizer.service.ProblemQueryService;
import com.quertimizer.service.ProblemWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LogFormatter logFormatter;
    private final ProblemQueryService problemQueryService;
    private final ProblemWorkspaceService problemWorkspaceService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String actor = resolveActor(session);

        // 연결 완료 로그와 초기 연결 메시지 전송
        log.info("{}", logFormatter.formatWebSocketLine(actor, "Problem WebSocket connection open", null));
        sendMessage(session, ProblemExecuteRes.connected(actor));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "Problem WebSocket client-send", null));
        logLines(logFormatter.formatRequestBodyLines(prefix, message.getPayload()));

        try {
            ProblemSocketReq request = objectMapper.readValue(message.getPayload(), ProblemSocketReq.class);
            if (request.type() == null || request.type().isBlank()) {
                throw new IllegalArgumentException("문제 실행 요청 타입이 비어 있다.");
            }

            switch (request.type()) {
                case "problem.execute" -> handleExecute(session, request);
                case "problem.leave" -> handleLeave(session, request);
                default -> sendMessage(session, ProblemExecuteRes.error("지원하지 않는 문제 소켓 요청이다."));
            }
        } catch (Exception exception) {
            sendMessage(session, ProblemExecuteRes.error(resolveErrorMessage(exception)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String actor = resolveActor(session);

        // 연결 종료 후 작업용 스키마 정리 예약
        problemWorkspaceService.handleConnectionClose(session.getId());
        log.info("{}", logFormatter.formatWebSocketLine(actor, "Problem WebSocket connection close", status.toString()));
    }

    public void closeProblemSocket(WebSocketSession session) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.NORMAL);
        }
    }

    private void handleExecute(WebSocketSession session, ProblemSocketReq request) throws Exception {
        String userId = resolveAuthenticatedUserId(session);
        ProblemQueryService.QueryExecutionResult executionResult = problemQueryService.executeInteractiveSql(
                userId,
                session.getId(),
                request.problemId(),
                request.sql(),
                resolveDbmsType(request.dbms())
        );

        sendMessage(session, ProblemExecuteRes.executionSuccess(
                executionResult.problemId(),
                executionResult.mode(),
                executionResult.message(),
                executionResult.columns(),
                executionResult.rows(),
                executionResult.planLines(),
                executionResult.rowCount(),
                executionResult.executionTimeMs()
        ));
    }

    private void handleLeave(WebSocketSession session, ProblemSocketReq request) throws Exception {

        // 명시적 페이지 이탈 시 작업용 스키마 정리
        problemWorkspaceService.handleExplicitLeave(session.getId());
        sendMessage(session, ProblemExecuteRes.leaveSuccess(request.problemId()));
    }

    private void sendMessage(WebSocketSession session, ProblemExecuteRes payload) throws Exception {
        String actor = resolveActor(session);
        String prefix = logFormatter.prefix(actor);
        String serializedPayload = objectMapper.writeValueAsString(payload);

        log.info("{}", logFormatter.formatWebSocketLine(actor, "Problem WebSocket server-send", null));
        logLines(logFormatter.formatResponseBodyLines(prefix, serializedPayload));
        session.sendMessage(new TextMessage(serializedPayload));
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
}
