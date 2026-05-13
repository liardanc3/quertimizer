package com.quertimizer.judge.domain.model;

import lombok.Getter;

@Getter
public enum DatabaseNodeFailReason {

    DATABASE_NODE_CONFIG_NOT_FOUND("DB 노드 설정 없음"),
    INVALID_MAX_CONCURRENCY("max concurrency는 1 이상 필요");

    private final String message;

    DatabaseNodeFailReason(String message) {
        this.message = message;
    }
}
