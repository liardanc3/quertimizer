package com.quertimizer.monitoring.application.input;

import com.quertimizer.monitoring.domain.model.MonitoringLogConstant;
import com.quertimizer.monitoring.domain.model.MonitoringLogLevel;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MonitoringLogSearchInput {

    private final MonitoringLogLevel level;
    private final LocalDate date;
    private final int size;

    public static MonitoringLogSearchInput of(String level, String date, Integer size) {
        // 외부 요청값을 로그 조회 입력으로 정규화
        MonitoringLogLevel resolvedLevel = MonitoringLogLevel.fromValueOrDefault(level, MonitoringLogLevel.INFO);
        LocalDate resolvedDate = date != null && !date.isBlank() ? LocalDate.parse(date.trim()) : LocalDate.now();
        int resolvedSize = size != null ? Math.max(1, Math.min(size, MonitoringLogConstant.MAX_LOG_LINE_SIZE)) : MonitoringLogConstant.DEFAULT_LOG_LINE_SIZE;
        return new MonitoringLogSearchInput(resolvedLevel, resolvedDate, resolvedSize);
    }
}
