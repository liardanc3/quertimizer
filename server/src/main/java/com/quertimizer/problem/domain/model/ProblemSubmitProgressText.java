package com.quertimizer.problem.domain.model;

public enum ProblemSubmitProgressText {

    SQL_VALIDATE_RUNNING("SQL 구문 정적 검사 중"),
    SQL_VALIDATE_FAILED("SQL 오류 검사 실패"),
    SQL_VALIDATE_SUCCESS("SQL 구문 정적 검사 완료"),
    ENVIRONMENT_RUNNING("채점환경 구성 대기 중"),
    ENVIRONMENT_WAITING("채점환경 구성 대기 중"),
    ENVIRONMENT_FAILED("채점환경 구성 실패"),
    ENVIRONMENT_SUCCESS("채점환경 구성 완료"),
    ANSWER_VALIDATE_RUNNING("출력 데이터 검증 중"),
    ANSWER_VALIDATE_FAILED("출력 데이터 검증 실패"),
    ANSWER_INCORRECT("출력 데이터 오답"),
    ANSWER_CORRECT("출력 데이터 정답"),
    CORRECT_ANSWER("정답"),
    INCORRECT_ANSWER("오답"),
    DDL_RUNNING("인덱스 변경 반영 중"),
    DDL_EMPTY("인덱스 변경내용 없음"),
    DDL_SUCCESS("인덱스 변경 반영 완료"),
    DDL_FAILED("인덱스 변경 반영 실패"),
    PLAN_RUNNING("실행계획 분석 중"),
    PLAN_SUCCESS("실행계획 분석 완료"),
    PLAN_FAILED("실행계획 분석 실패"),
    CONNECTION_RETRY("DB 커넥션 연결 오류. 2초 후 재시도"),
    SQL_VALIDATE_UNEXPECTED_FAILURE("SQL 오류 검사에 실패했습니다."),
    ANSWER_VALIDATE_UNEXPECTED_FAILURE("출력 데이터 검사에 실패했습니다."),
    DDL_UNEXPECTED_FAILURE("인덱스 변경 반영에 실패했습니다."),
    PLAN_UNEXPECTED_FAILURE("실행계획 분석에 실패했습니다."),
    COST_PREFIX("Cost · "),
    CHECK_PREFIX("✓ "),
    PLAN_FULL_SCAN("✓ Scan · Full Scan"),
    PLAN_INDEX_SCAN("✓ Scan · Index Scan"),
    PLAN_BITMAP_SCAN("✓ Scan · Bitmap Scan"),
    PLAN_TID_SCAN("✓ Scan · Tid Scan"),
    PLAN_DERIVED_SCAN("✓ Scan · Derived Scan"),
    PLAN_NESTED_LOOP("✓ Join · Nested Loop"),
    PLAN_MERGE_JOIN("✓ Join · Merge Join"),
    PLAN_HASH_JOIN("✓ Join · Hash Join"),
    PLAN_ACCESS_FILTER("✓ Filter · Access Filter"),
    PLAN_POST_FILTER("✓ Filter · Post Filter"),
    PLAN_PLAIN_SORT("✓ Sort · Plain Sort"),
    PLAN_INCREMENTAL_SORT("✓ Sort · Incremental Sort"),
    PLAN_HASH_AGG("✓ Aggregate · Hash Agg"),
    PLAN_GROUP_AGG("✓ Aggregate · Group Agg"),
    PLAN_UNIQUE_AGG("✓ Aggregate · Unique Agg"),
    PLAN_HINT_USED("✓ Hint · Used");

    private final String text;

    ProblemSubmitProgressText(String text) {
        this.text = text;
    }

    public String getText() {
        // 문구 조회
        return text;
    }

}
