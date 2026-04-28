package com.quertimizer.problem.domain.model;

public enum ProblemAnswerFailReason {

    HASH_CREATION_FAILED("SHA-512 해시를 생성할 수 없다.");

    private final String message;

    ProblemAnswerFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지를 반환한다
        return message;
    }

}
