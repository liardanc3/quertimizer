package com.quertimizer.global.realtime.registry;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionSocketRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionSockets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSockets = new ConcurrentHashMap<>();

    /**
     * HttpSession ID 기준 WebSocket 연결을 등록한다.
     *
     * @param sessionId WebSocket과 연결된 HTTP 세션 ID
     * @param session 등록할 WebSocket 세션
     */
    public void registerSessionSocket(String sessionId, WebSocketSession session) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        sessionSockets.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * 사용자 handle 기준 WebSocket 연결을 등록한다.
     *
     * @param handle WebSocket과 연결된 사용자 handle
     * @param session 등록할 WebSocket 세션
     */
    public void registerUserSocket(String handle, WebSocketSession session) {
        if (handle == null || handle.isBlank()) {
            return;
        }

        userSockets.computeIfAbsent(handle, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * HttpSession ID 기준 WebSocket 연결을 해제한다.
     *
     * @param sessionId WebSocket과 연결된 HTTP 세션 ID
     * @param session 해제할 WebSocket 세션
     */
    public void unregisterSessionSocket(String sessionId, WebSocketSession session) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        sessionSockets.computeIfPresent(sessionId, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /**
     * 사용자 handle 기준 WebSocket 연결을 해제한다.
     *
     * @param handle WebSocket과 연결된 사용자 handle
     * @param session 해제할 WebSocket 세션
     */
    public void unregisterUserSocket(String handle, WebSocketSession session) {
        if (handle == null || handle.isBlank()) {
            return;
        }

        userSockets.computeIfPresent(handle, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /**
     * 로그아웃한 HTTP 세션의 WebSocket 연결 목록을 제거하고 반환한다.
     *
     * @param sessionId 제거할 HTTP 세션 ID
     */
    public Set<WebSocketSession> takeSessionSockets(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Set.of();
        }

        Set<WebSocketSession> sessions = sessionSockets.remove(sessionId);
        return sessions != null ? Set.copyOf(sessions) : Set.of();
    }

    /**
     * 사용자 handle 기준 WebSocket 연결 목록을 반환한다.
     *
     * @param handle 조회할 사용자 handle
     */
    public Set<WebSocketSession> findUserSockets(String handle) {
        if (handle == null || handle.isBlank()) {
            return Set.of();
        }

        Set<WebSocketSession> sessions = userSockets.get(handle);
        return sessions != null ? Set.copyOf(sessions) : Set.of();
    }
}
