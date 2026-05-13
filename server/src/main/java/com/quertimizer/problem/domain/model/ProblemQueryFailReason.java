package com.quertimizer.problem.domain.model;

public enum ProblemQueryFailReason {

    SINGLE_SQL_ONLY("한 번에 하나의 SQL만 실행할 수 있습니다."),
    SQL_REQUIRED("실행할 SQL을 입력해 주세요."),
    SQL_LENGTH_EXCEEDED("SQL 길이 제한을 초과했습니다."),
    TEMPLATE_TABLE_ACCESS_DENIED("템플릿 테이블에는 직접 접근할 수 없습니다."),
    BASE_WORKSPACE_ACCESS_DENIED("원본 테이블셋에는 직접 접근할 수 없습니다."),
    OTHER_SESSION_SCHEMA_ACCESS_DENIED("다른 세션 스키마에는 접근할 수 없습니다."),
    OTHER_USER_WORKSPACE_ACCESS_DENIED("다른 사용자 작업용 스키마에는 접근할 수 없습니다."),
    ALTER_SYSTEM_UNAVAILABLE("ALTER SYSTEM은 사용할 수 없습니다."),
    COPY_UNAVAILABLE("COPY는 사용할 수 없습니다."),
    PROGRAM_UNAVAILABLE("PROGRAM은 사용할 수 없습니다."),
    CREATE_EXTENSION_UNAVAILABLE("CREATE EXTENSION은 사용할 수 없습니다."),
    DROP_SCHEMA_UNAVAILABLE("DROP SCHEMA는 사용할 수 없습니다."),
    DROP_TABLE_UNAVAILABLE("DROP TABLE은 사용할 수 없습니다."),
    TRUNCATE_UNAVAILABLE("TRUNCATE는 사용할 수 없습니다."),
    VACUUM_UNAVAILABLE("VACUUM은 사용할 수 없습니다."),
    REINDEX_UNAVAILABLE("REINDEX는 사용할 수 없습니다."),
    PG_CATALOG_ACCESS_DENIED("pg_catalog에는 접근할 수 없습니다."),
    INFORMATION_SCHEMA_ACCESS_DENIED("information_schema에는 접근할 수 없습니다."),
    CONCURRENTLY_UNAVAILABLE("CONCURRENTLY는 사용할 수 없습니다."),
    SELECT_RESULT_UNAVAILABLE("SELECT 결과를 확인할 수 없습니다."),
    PLAN_RESULT_UNAVAILABLE("실행 계획을 확인할 수 없습니다."),
    WRITE_CTE_UNSUPPORTED("데이터를 수정하는 CTE는 지원하지 않습니다."),
    UNSUPPORTED_SQL_COMMAND("SELECT, EXPLAIN, EXPLAIN ANALYZE, CREATE INDEX, DROP INDEX, ALTER INDEX만 실행할 수 있습니다."),
    SUBMIT_SELECT_ONLY("제출은 SELECT 1개만 가능합니다."),
    SUBMIT_SELECT_FOLLOWED_BY_STATEMENTS("제출에서는 SELECT 아래 구문을 함께 보낼 수 없습니다."),
    SUBMIT_SQL_REQUIRED("제출할 SQL을 입력해 주세요."),
    SUBMIT_SELECT_REQUIRED("제출에는 최소 한 개의 SELECT가 필요합니다."),
    SUBMIT_REFERENCE_SELECT_NOT_FOUND("제출 기준 SELECT를 찾을 수 없습니다."),
    SUBMIT_SELECT_AND_INDEX_DDL_ONLY("제출은 SELECT 1개와 INDEX DDL만 지원합니다."),
    EXECUTE_SQL_WITH_INDEX_DDL_SINGLE_ONLY("실행 SQL은 INDEX DDL 뒤에 하나의 실행문만 허용됩니다."),
    PROBLEM_INFO_NOT_FOUND("문제 정보를 찾을 수 없습니다."),
    ANSWER_HASH_NOT_REGISTERED("정답 해시가 등록되지 않았습니다."),
    ANSWER_CASE_NOT_FOUND("출력 데이터 검증 케이스가 없습니다."),
    OFFICIAL_COST_SELECTION_FAILED("공식 비용 측정 결과를 선택하지 못했습니다."),
    PROBLEM_EXAMPLE_SERIALIZATION_FAILED("문제 예시 직렬화 실패"),
    SUBMIT_FAILED("SQL 제출에 실패했습니다."),
    OUTPUT_PREVIEW_RATE_LIMITED("SQL 실행 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    RETRY_WAIT_INTERRUPTED("재시도 대기 중 인터럽트가 발생했습니다.");

    private final String message;

    ProblemQueryFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지 반환
        return message;
    }

}
