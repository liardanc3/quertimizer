package com.quertimizer.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class BusinessException extends RuntimeException {

    private final String reason;
    private final HttpStatusCode statusCode;

    public BusinessException(String reason, HttpStatusCode statusCode) {
        this.reason = reason;
        this.statusCode = statusCode;
    }
}
