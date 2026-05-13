package com.quertimizer.problem.domain.model;

public final class ProblemCreateProgressText {

    private ProblemCreateProgressText() {
    }

    public static String hiddenDataKey(int sequence) {
        // 숨김 데이터 진행 단계 키 생성
        return "hidden-data-" + sequence;
    }

    public static String hiddenDataRunningMessage(int sequence) {
        // 숨김 데이터 생성 중 메시지 생성
        return "채점용 데이터 INSERT - Hidden " + sequence + " 생성 중";
    }

    public static String hiddenDataSuccessMessage(int sequence) {
        // 숨김 데이터 생성 완료 메시지 생성
        return "채점용 데이터 INSERT - Hidden " + sequence + " 생성 완료";
    }

}
