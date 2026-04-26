package com.quertimizer.global.realtime.registry;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionSocketRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionSockets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> userSockets = new ConcurrentHashMap<>();

    public void registerSessionSocket(String sessionId, WebSocketSession session) {
        // HttpSession 기준 WebSocket 연결을 등록
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        sessionSockets.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void registerUserSocket(String handle, WebSocketSession session) {
        // 사용자 Handle 기준 WebSocket 연결을 등록
        if (handle == null || handle.isBlank()) {
            return;
        }

        userSockets.computeIfAbsent(handle, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterSessionSocket(String sessionId, WebSocketSession session) {
        // HttpSession 기준 WebSocket 연결을 해제
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        sessionSockets.computeIfPresent(sessionId, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void unregisterUserSocket(String handle, WebSocketSession session) {
        // 사용자 Handle 기준 WebSocket 연결을 해제
        if (handle == null || handle.isBlank()) {
            return;
        }

        userSockets.computeIfPresent(handle, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public Set<WebSocketSession> takeSessionSockets(String sessionId) {
        // 로그아웃 시 HttpSession 기준 WebSocket 연결 목록을 제거 후 반환
        if (sessionId == null || sessionId.isBlank()) {
            return Set.of();
        }

        Set<WebSocketSession> sessions = sessionSockets.remove(sessionId);
        return sessions != null ? Set.copyOf(sessions) : Set.of();
    }

    public Set<WebSocketSession> findUserSockets(String handle) {
        // 사용자 Handle 기준 WebSocket 연결 목록을 조회
        if (handle == null || handle.isBlank()) {
            return Set.of();
        }

        Set<WebSocketSession> sessions = userSockets.get(handle);
        return sessions != null ? Set.copyOf(sessions) : Set.of();
    }
}
