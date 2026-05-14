package com.quertimizer.judge.application.exception;

import lombok.Getter;

@Getter
public class UserSqlExecutionException extends RuntimeException {

    private final String reason;

    public UserSqlExecutionException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }
}
