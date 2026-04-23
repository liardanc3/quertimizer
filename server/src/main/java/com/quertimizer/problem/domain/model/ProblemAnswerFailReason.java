package com.quertimizer.problem.domain.model;

public enum ProblemAnswerFailReason {

    HASH_CREATION_FAILED("SHA-512 해시를 생성할 수 없다.");

    private final String message;

    ProblemAnswerFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
