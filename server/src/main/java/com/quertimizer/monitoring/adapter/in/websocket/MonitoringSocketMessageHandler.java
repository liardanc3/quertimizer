package com.quertimizer.monitoring.adapter.in.websocket;

import com.quertimizer.global.log.LogMdcContext;
import com.quertimizer.global.websocket.sender.WebSocketSender;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringDatabaseStatusSocketRes;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringLogSocketReq;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringLogSocketRes;
import com.quertimizer.monitoring.adapter.in.websocket.dto.MonitoringResourceSocketRes;
import com.quertimizer.monitoring.adapter.in.http.response.DatabaseStatusRes;
import com.quertimizer.monitoring.adapter.in.http.response.ServerLogRes;
import com.quertimizer.monitoring.adapter.in.http.response.SystemResourceRes;
import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.port.in.GetDatabaseStatusUseCase;
import com.quertimizer.monitoring.application.port.in.GetServerLogsUseCase;
import com.quertimizer.monitoring.application.port.in.GetSystemResourcesUseCase;
import com.quertimizer.monitoring.domain.model.MonitoringFailReason;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MonitoringSocketMessageHandler {

    private final GetSystemResourcesUseCase getSystemResources;
    private final GetDatabaseStatusUseCase getDatabaseStatus;
    private final GetServerLogsUseCase getServerLogs;
    private final WebSocketSender webSocketSender;
    private final ScheduledExecutorService logSubscriptionExecutor =
            Executors.newSingleThreadScheduledExecutor(new MonitoringLogThreadFactory());
    private final Map<String, ScheduledFuture<?>> logSubscriptionsBySessionId = new ConcurrentHashMap<>();

    /**
     * 관리자 서버 리소스 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상 확인
     *   <li>서버 리소스 조회
     *   <li>조회 결과 전송
     * </ol>
     *
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping("monitoring.resources")
    public void handleMonitoringResources(SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = createReplyTarget(headerAccessor);

        try {
            webSocketSender.sendToSession(
                    replyTarget.handle, replyTarget.sessionId,
                    MonitoringResourceSocketRes.success(SystemResourceRes.from(getSystemResources.execute()))
            );
        } catch (Exception exception) {
            sendResourceError(replyTarget, exception);
        }
    }

    /**
     * 관리자 DB 실행 환경 WebSocket 요청을 처리한다.
     *
     * <ol>
     *   <li>응답 대상 확인
     *   <li>DB 실행 환경 조회
     *   <li>조회 결과 전송
     * </ol>
     *
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping("monitoring.database-status")
    public void handleMonitoringDatabaseStatus(SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = createReplyTarget(headerAccessor);

        try {
            webSocketSender.sendToSession(
                    replyTarget.handle, replyTarget.sessionId,
                    MonitoringDatabaseStatusSocketRes.success(DatabaseStatusRes.from(getDatabaseStatus.execute()))
            );
        } catch (Exception exception) {
            sendDatabaseStatusError(replyTarget, exception);
        }
    }

    /**
     * 관리자 서버 로그 WebSocket 구독을 처리한다.
     *
     * <ol>
     *   <li>기존 로그 구독 해제
     *   <li>현재 로그 스냅샷 전송
     *   <li>추가 로그 라인 주기 전송
     * </ol>
     *
     * @param request 로그 레벨, 날짜, 조회 줄 수 요청
     * @param headerAccessor 인증 handle 확인과 응답 전송에 사용하는 WebSocket 헤더
     */
    @MessageMapping("monitoring.logs.subscribe")
    public void handleMonitoringLogsSubscribe(MonitoringLogSocketReq request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketReplyTarget replyTarget = createReplyTarget(headerAccessor);
        cancelLogSubscription(replyTarget.sessionId);

        try {
            MonitoringLogSocketReq safeRequest = request != null ? request : new MonitoringLogSocketReq();
            MonitoringLogSearchInput input = MonitoringLogSearchInput.of(safeRequest.getLevel(), safeRequest.getDate(), safeRequest.getSize());
            ServerLogRes snapshot = ServerLogRes.from(getServerLogs.execute(input));
            webSocketSender.sendToSessionSilently(replyTarget.handle, replyTarget.sessionId, MonitoringLogSocketRes.snapshot(snapshot));

            AtomicReference<List<String>> previousLines = new AtomicReference<>(snapshot.getLines());
            ScheduledFuture<?> subscription = logSubscriptionExecutor.scheduleWithFixedDelay(
                    LogMdcContext.wrap(() -> sendAppendedLogLines(replyTarget, input, previousLines)),
                    1, 1, TimeUnit.SECONDS
            );
            logSubscriptionsBySessionId.put(replyTarget.sessionId, subscription);
        } catch (Exception exception) {
            sendLogError(replyTarget, exception);
        }
    }

    /**
     * 관리자 서버 로그 WebSocket 구독 해제를 처리한다.
     *
     * @param headerAccessor WebSocket 세션 ID 확인에 사용하는 WebSocket 헤더
     */
    @MessageMapping("monitoring.logs.unsubscribe")
    public void handleMonitoringLogsUnsubscribe(SimpMessageHeaderAccessor headerAccessor) {
        cancelLogSubscription(headerAccessor.getSessionId());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        // 연결이 끊긴 WebSocket 세션의 로그 구독 해제
        cancelLogSubscription(event.getSessionId());
    }

    @PreDestroy
    public void shutdownLogSubscriptionExecutor() {
        // 로그 구독 스케줄러 종료
        logSubscriptionExecutor.shutdownNow();
    }

    private WebSocketReplyTarget createReplyTarget(SimpMessageHeaderAccessor headerAccessor) {
        // WebSocket 세션 ID와 인증 handle 확인
        String sessionId = headerAccessor.getSessionId();
        String handle = resolveHandle(headerAccessor);
        if (sessionId == null || sessionId.isBlank() || handle == null || handle.isBlank()) {
            throw new IllegalStateException(MonitoringFailReason.LOGIN_INFORMATION_NOT_FOUND.getMessage());
        }

        return new WebSocketReplyTarget(handle, sessionId);
    }

    private String resolveHandle(SimpMessageHeaderAccessor headerAccessor) {
        // WebSocket 세션 attribute 기준 handle 확인
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String handle = sessionAttributes != null ? (String) sessionAttributes.get("handle") : null;
        if (handle != null && !handle.isBlank()) {
            return handle;
        }

        // WebSocket principal 기준 handle 확인
        Principal user = headerAccessor.getUser();
        return user != null ? user.getName() : null;
    }

    private void sendResourceError(WebSocketReplyTarget replyTarget, Exception exception) {
        // 서버 리소스 조회 실패 응답 전송
        try {
            webSocketSender.sendToSession(
                    replyTarget.handle, replyTarget.sessionId,
                    MonitoringResourceSocketRes.error(resolveMessage(exception, MonitoringFailReason.SYSTEM_RESOURCE_LOAD_FAILED))
            );
        } catch (Exception sendException) {
            log.warn("서버 리소스 WebSocket 오류 응답 전송 실패", sendException);
        }
    }

    private void sendDatabaseStatusError(WebSocketReplyTarget replyTarget, Exception exception) {
        // DB 실행 환경 조회 실패 응답 전송
        try {
            webSocketSender.sendToSession(
                    replyTarget.handle, replyTarget.sessionId,
                    MonitoringDatabaseStatusSocketRes.error(resolveMessage(exception, MonitoringFailReason.DATABASE_STATUS_LOAD_FAILED))
            );
        } catch (Exception sendException) {
            log.warn("DB 상태 WebSocket 오류 응답 전송 실패", sendException);
        }
    }

    private void sendAppendedLogLines(WebSocketReplyTarget replyTarget,
                                      MonitoringLogSearchInput input, AtomicReference<List<String>> previousLines) {
        // 이전 스냅샷 이후 추가된 로그 라인만 현재 세션으로 전송
        try {
            List<String> currentLines = getServerLogs.execute(input).getLines();
            List<String> appendedLines = findAppendedLines(previousLines.get(), currentLines);
            previousLines.set(currentLines);
            if (!appendedLines.isEmpty()) {
                webSocketSender.sendToSessionSilently(replyTarget.handle, replyTarget.sessionId, MonitoringLogSocketRes.append(appendedLines));
            }
        } catch (Exception exception) {
            sendLogError(replyTarget, exception);
            cancelLogSubscription(replyTarget.sessionId);
        }
    }

    private List<String> findAppendedLines(List<String> previousLines, List<String> currentLines) {
        // tail 조회 범위가 밀려도 겹치는 마지막 라인 이후부터 추가 라인 판별
        if (currentLines.isEmpty()) {
            return List.of();
        }
        if (previousLines.isEmpty()) {
            return currentLines;
        }

        int overlap = findOverlapSize(previousLines, currentLines);
        return overlap >= currentLines.size() ? List.of() : currentLines.subList(overlap, currentLines.size());
    }

    private int findOverlapSize(List<String> previousLines, List<String> currentLines) {
        // 이전 로그 suffix와 현재 로그 prefix가 가장 길게 겹치는 크기 계산
        int maxOverlap = Math.min(previousLines.size(), currentLines.size());
        for (int overlap = maxOverlap; overlap > 0; overlap--) {
            if (matchesOverlap(previousLines, currentLines, overlap)) {
                return overlap;
            }
        }

        return 0;
    }

    private boolean matchesOverlap(List<String> previousLines, List<String> currentLines, int overlap) {
        // 지정한 길이만큼 이전 suffix와 현재 prefix 일치 여부 확인
        int previousStart = previousLines.size() - overlap;
        for (int index = 0; index < overlap; index++) {
            if (!previousLines.get(previousStart + index).equals(currentLines.get(index))) {
                return false;
            }
        }

        return true;
    }

    private void sendLogError(WebSocketReplyTarget replyTarget, Exception exception) {
        // 서버 로그 조회 실패 응답 전송
        try {
            webSocketSender.sendToSessionSilently(
                    replyTarget.handle, replyTarget.sessionId,
                    MonitoringLogSocketRes.error(resolveMessage(exception, MonitoringFailReason.SERVER_LOG_LOAD_FAILED))
            );
        } catch (Exception sendException) {
            log.warn("서버 로그 WebSocket 오류 응답 전송 실패", sendException);
        }
    }

    private void cancelLogSubscription(String sessionId) {
        // WebSocket 세션 기준 로그 구독 스케줄 취소
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        ScheduledFuture<?> subscription = logSubscriptionsBySessionId.remove(sessionId);
        if (subscription != null) {
            subscription.cancel(false);
        }
    }

    private String resolveMessage(Exception exception, MonitoringFailReason fallbackReason) {
        // 예외 메시지 존재 시 그대로 사용
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return fallbackReason.getMessage();
    }

    private static final class WebSocketReplyTarget {
        private final String handle;
        private final String sessionId;

        private WebSocketReplyTarget(String handle, String sessionId) {
            this.handle = handle;
            this.sessionId = sessionId;
        }
    }

    private static final class MonitoringLogThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            // 모니터링 로그 구독 전용 데몬 스레드 생성
            Thread thread = new Thread(runnable, "monitoring-log-subscription");
            thread.setDaemon(true);
            return thread;
        }
    }
}
