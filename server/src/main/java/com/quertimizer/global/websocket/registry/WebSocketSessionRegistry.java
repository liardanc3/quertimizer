package com.quertimizer.global.websocket.registry;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, Set<String>> webSocketSessionIdsByHttpSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> httpSessionIdByWebSocketSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> handleByWebSocketSessionId = new ConcurrentHashMap<>();

    public void register(String httpSessionId, String handle, String webSocketSessionId) {
        // WebSocket 세션 등록 가능 여부 확인
        if (httpSessionId == null || httpSessionId.isBlank()
                || handle == null || handle.isBlank()
                || webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return;
        }

        // HTTP 세션과 WebSocket 세션 매핑 저장
        webSocketSessionIdsByHttpSessionId
                .computeIfAbsent(httpSessionId, key -> ConcurrentHashMap.newKeySet())
                .add(webSocketSessionId);
        httpSessionIdByWebSocketSessionId.put(webSocketSessionId, httpSessionId);
        handleByWebSocketSessionId.put(webSocketSessionId, handle);
    }

    public void unregister(String webSocketSessionId) {
        // WebSocket 세션 ID 기준 등록 정보 제거
        if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return;
        }

        String httpSessionId = httpSessionIdByWebSocketSessionId.remove(webSocketSessionId);
        handleByWebSocketSessionId.remove(webSocketSessionId);

        // HTTP 세션에 묶인 WebSocket 세션 목록에서 제거
        if (httpSessionId != null && !httpSessionId.isBlank()) {
            webSocketSessionIdsByHttpSessionId.computeIfPresent(httpSessionId, (key, webSocketSessionIds) -> {
                webSocketSessionIds.remove(webSocketSessionId);
                return webSocketSessionIds.isEmpty() ? null : webSocketSessionIds;
            });
        }
    }

    public Map<String, Set<String>> takeHttpSessionTargets(String httpSessionId) {
        // HTTP 세션 기준 종료 대상 WebSocket 세션 목록 조회
        if (httpSessionId == null || httpSessionId.isBlank()) {
            return Map.of();
        }

        Set<String> webSocketSessionIds = webSocketSessionIdsByHttpSessionId.remove(httpSessionId);
        if (webSocketSessionIds == null || webSocketSessionIds.isEmpty()) {
            return Map.of();
        }

        // handle별 WebSocket 세션 ID 목록 구성 후 registry에서 제거
        Map<String, Set<String>> targetsByHandle = new HashMap<>();
        for (String webSocketSessionId : webSocketSessionIds) {
            httpSessionIdByWebSocketSessionId.remove(webSocketSessionId);
            String handle = handleByWebSocketSessionId.remove(webSocketSessionId);
            if (handle == null || handle.isBlank()) {
                continue;
            }

            targetsByHandle.computeIfAbsent(handle, key -> ConcurrentHashMap.newKeySet()).add(webSocketSessionId);
        }

        return targetsByHandle;
    }

    public Set<String> findSessionIdsByHandle(String handle) {
        // handle 기준 현재 등록된 WebSocket 세션 ID 목록 조회
        if (handle == null || handle.isBlank()) {
            return Set.of();
        }

        Set<String> webSocketSessionIds = ConcurrentHashMap.newKeySet();
        handleByWebSocketSessionId.forEach((webSocketSessionId, registeredHandle) -> {
            if (handle.equals(registeredHandle)) {
                webSocketSessionIds.add(webSocketSessionId);
            }
        });
        return webSocketSessionIds;
    }
}
