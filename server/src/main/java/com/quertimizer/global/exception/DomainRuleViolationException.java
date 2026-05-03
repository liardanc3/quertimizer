package com.quertimizer.global.exception;

import lombok.Getter;

@Getter
public class DomainRuleViolationException extends RuntimeException {

    private final DomainRuleViolationType type;

    public DomainRuleViolationException(String reason, DomainRuleViolationType type) {
        super(reason);
        this.type = type;
    }
}
