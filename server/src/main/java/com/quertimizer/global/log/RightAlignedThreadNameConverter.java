package com.quertimizer.global.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class RightAlignedThreadNameConverter extends ClassicConverter {

    private static final int THREAD_NAME_WIDTH = 15;

    @Override
    public String convert(ILoggingEvent event) {
        // 스레드명 없음 시 공백 폭 반환
        String threadName = event.getThreadName();
        if (threadName == null || threadName.isBlank()) {
            return String.format("%" + THREAD_NAME_WIDTH + "s", "");
        }

        // 스레드명 마지막 15자 기준 정규화 후 우측 정렬
        String normalizedThreadName = threadName.length() <= THREAD_NAME_WIDTH
                ? threadName
                : threadName.substring(threadName.length() - THREAD_NAME_WIDTH);
        return String.format("%" + THREAD_NAME_WIDTH + "s", normalizedThreadName);
    }
}
