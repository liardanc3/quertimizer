package com.quertimizer.problem.domain.model;

public enum ProblemCreateProgressStep {

    OPEN_DATA("open-data", 1, "채점용 데이터 INSERT - Open 생성 중", "채점용 데이터 INSERT - Open 생성 완료"),
    ANSWER_HASH("answer-hash", 2, "정답 해시 생성 중", "정답 해시 생성 완료"),
    TABLE_INFO("table-info", 3, "테이블 정보 생성 중", "테이블 정보 생성 완료"),
    ERD_INFO("erd-info", 4, "ERD 정보 생성 중", "ERD 정보 생성 완료"),
    DATA_EXAMPLE("data-example", 5, "데이터 예시 생성 중", "데이터 예시 생성 완료"),
    OUTPUT_EXAMPLE("output-example", 6, "출력 예시 생성 중", "출력 예시 생성 완료");

    private final String key;
    private final int order;
    private final String runningMessage;
    private final String successMessage;

    ProblemCreateProgressStep(String key, int order, String runningMessage, String successMessage) {
        this.key = key;
        this.order = order;
        this.runningMessage = runningMessage;
        this.successMessage = successMessage;
    }

    public String getKey() {
        // 진행 단계 키 조회
        return key;
    }

    public int getOrder() {
        // 진행 단계 순서 조회
        return order;
    }

    public String getRunningMessage() {
        // 진행 중 메시지 조회
        return runningMessage;
    }

    public String getSuccessMessage() {
        // 진행 완료 메시지 조회
        return successMessage;
    }

}
