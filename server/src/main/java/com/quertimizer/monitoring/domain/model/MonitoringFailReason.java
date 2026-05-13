package com.quertimizer.monitoring.domain.model;

import lombok.Getter;

@Getter
public enum MonitoringFailReason {

    LOGIN_INFORMATION_NOT_FOUND("로그인 정보 없음"),
    SYSTEM_RESOURCE_LOAD_FAILED("서버 리소스 조회 실패"),
    DATABASE_STATUS_LOAD_FAILED("DB 상태 조회 실패"),
    SERVER_LOG_LOAD_FAILED("서버 로그 조회 실패");

    private final String message;

    MonitoringFailReason(String message) {
        this.message = message;
    }
}
