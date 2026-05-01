package com.quertimizer.global.realtime.registry;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStompRegistry {

    private final ConcurrentHashMap<String, Set<String>> stompSessionIdsByHttpSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> httpSessionIdByStompSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> handleByStompSessionId = new ConcurrentHashMap<>();

    public void register(String httpSessionId, String handle, String stompSessionId) {
        // STOMP 세션 등록 가능 여부 확인
        if (httpSessionId == null || httpSessionId.isBlank()
                || handle == null || handle.isBlank()
                || stompSessionId == null || stompSessionId.isBlank()) {
            return;
        }

        // HTTP 세션과 STOMP 세션 매핑 저장
        stompSessionIdsByHttpSessionId
                .computeIfAbsent(httpSessionId, key -> ConcurrentHashMap.newKeySet())
                .add(stompSessionId);
        httpSessionIdByStompSessionId.put(stompSessionId, httpSessionId);
        handleByStompSessionId.put(stompSessionId, handle);
    }

    public void unregister(String stompSessionId) {
        // STOMP 세션 ID 기준 등록 정보 제거
        if (stompSessionId == null || stompSessionId.isBlank()) {
            return;
        }

        String httpSessionId = httpSessionIdByStompSessionId.remove(stompSessionId);
        handleByStompSessionId.remove(stompSessionId);

        // HTTP 세션에 묶인 STOMP 세션 목록에서 제거
        if (httpSessionId != null && !httpSessionId.isBlank()) {
            stompSessionIdsByHttpSessionId.computeIfPresent(httpSessionId, (key, stompSessionIds) -> {
                stompSessionIds.remove(stompSessionId);
                return stompSessionIds.isEmpty() ? null : stompSessionIds;
            });
        }
    }

    public Map<String, Set<String>> takeHttpSessionTargets(String httpSessionId) {
        // HTTP 세션 기준 종료 대상 STOMP 세션 목록 조회
        if (httpSessionId == null || httpSessionId.isBlank()) {
            return Map.of();
        }

        Set<String> stompSessionIds = stompSessionIdsByHttpSessionId.remove(httpSessionId);
        if (stompSessionIds == null || stompSessionIds.isEmpty()) {
            return Map.of();
        }

        // handle별 STOMP 세션 ID 목록 구성 후 registry에서 제거
        Map<String, Set<String>> targetsByHandle = new HashMap<>();
        for (String stompSessionId : stompSessionIds) {
            httpSessionIdByStompSessionId.remove(stompSessionId);
            String handle = handleByStompSessionId.remove(stompSessionId);
            if (handle == null || handle.isBlank()) {
                continue;
            }

            targetsByHandle.computeIfAbsent(handle, key -> ConcurrentHashMap.newKeySet()).add(stompSessionId);
        }

        return targetsByHandle;
    }
}
