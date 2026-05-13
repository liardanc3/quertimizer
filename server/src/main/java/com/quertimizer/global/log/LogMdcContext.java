package com.quertimizer.global.log;

import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

public final class LogMdcContext {

    private static final String MDC_ACTOR_KEY = "logActor";
    private static final int ACTOR_WIDTH = 15;

    private LogMdcContext() {
    }

    public static LogActorScope openActorScope(String actor) {
        // 기존 MDC actor 저장 후 현재 actor 반영
        String previousActor = MDC.get(MDC_ACTOR_KEY);
        if (actor == null || actor.isBlank()) {
            MDC.remove(MDC_ACTOR_KEY);
        } else {
            MDC.put(MDC_ACTOR_KEY, prefix(actor));
        }

        return new LogActorScope(previousActor);
    }

    public static Runnable wrap(Runnable runnable) {
        // 현재 스레드의 MDC context 캡처 후 Runnable 실행 시 복원
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> runWith(contextMap, runnable);
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        // 현재 스레드의 MDC context 캡처 후 Supplier 실행 시 복원
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> supplyWith(contextMap, supplier);
    }

    private static void runWith(Map<String, String> contextMap, Runnable runnable) {
        // 실행 전후 MDC context 백업과 복원
        Map<String, String> previousContextMap = MDC.getCopyOfContextMap();
        applyMdcContext(contextMap);
        try {
            runnable.run();
        } finally {
            applyMdcContext(previousContextMap);
        }
    }

    private static <T> T supplyWith(Map<String, String> contextMap, Supplier<T> supplier) {
        // 실행 전후 MDC context 백업과 복원
        Map<String, String> previousContextMap = MDC.getCopyOfContextMap();
        applyMdcContext(contextMap);
        try {
            return supplier.get();
        } finally {
            applyMdcContext(previousContextMap);
        }
    }

    private static void applyMdcContext(Map<String, String> contextMap) {
        // MDC context 없으면 정리하고 있으면 복원
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
            return;
        }

        MDC.setContextMap(contextMap);
    }

    private static String prefix(String actor) {
        // 로그 주체 prefix 생성
        return "[" + String.format("%" + ACTOR_WIDTH + "s", normalizeActor(actor)) + "] ";
    }

    private static String normalizeActor(String actor) {
        // 주체 정규화
        if (actor == null || actor.isBlank()) {
            return "unknown";
        }

        String normalizedActor = sanitizeControlCharacters(actor).split("%", 2)[0];
        if ("::1".equals(normalizedActor) || "0:0:0:0:0:0:0:1".equals(normalizedActor)) {
            normalizedActor = "127.0.0.1";
        }

        return normalizedActor.length() <= ACTOR_WIDTH
                ? normalizedActor
                : normalizedActor.substring(0, ACTOR_WIDTH);
    }

    private static String sanitizeControlCharacters(String value) {
        // 로그 제어문자 정리
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r') {
                builder.append("\\r");
                continue;
            }
            if (character == '\n') {
                builder.append("\\n");
                continue;
            }
            if (Character.isISOControl(character) && character != '\t') {
                builder.append('?');
                continue;
            }

            builder.append(character);
        }

        return builder.toString();
    }

    public static final class LogActorScope implements AutoCloseable {

        private final String previousActor;

        private LogActorScope(String previousActor) {
            this.previousActor = previousActor;
        }

        @Override
        public void close() {
            // 이전 MDC actor 복원
            if (previousActor == null || previousActor.isBlank()) {
                MDC.remove(MDC_ACTOR_KEY);
                return;
            }

            MDC.put(MDC_ACTOR_KEY, previousActor);
        }
    }
}
