package com.quertimizer.monitoring.domain.model;

import lombok.Getter;

@Getter
public enum MonitoringFailReason {

    JUDGE_CONFIG_NOT_FOUND("judge config 없음"),
    INVALID_MAX_CONCURRENCY("max concurrency는 1 이상 필요"),
    LOGIN_INFORMATION_NOT_FOUND("로그인 정보 없음"),
    SYSTEM_RESOURCE_LOAD_FAILED("서버 리소스 조회 실패"),
    DB_RUNTIME_LOAD_FAILED("DB Runtime 조회 실패"),
    SERVER_LOG_LOAD_FAILED("서버 로그 조회 실패");

    private final String message;

    MonitoringFailReason(String message) {
        this.message = message;
    }
}
