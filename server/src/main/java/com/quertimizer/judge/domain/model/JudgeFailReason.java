package com.quertimizer.judge.domain.model;

import lombok.Getter;

@Getter
public enum JudgeFailReason {

    SQL_RESULT_HASH_CREATION_FAILED("SQL 결과 해시 생성 실패"),
    LVM_SNAPSHOT_DEFAULT_CONFIG_NOT_FOUND("LVM 스냅샷 기본 설정이 없습니다."),
    UNKNOWN_ENVIRONMENT_ID("알 수 없는 실행 환경 ID: %s"),
    DROPPED_ENVIRONMENT("이미 제거된 실행 환경입니다: %s"),
    ISOLATED_SQL_EXECUTION_FAILED("격리 SQL 실행 실패"),
    SQL_EXECUTION_FAILED("SQL 실행 실패"),
    SETUP_SQL_EXECUTION_FAILED("설정 SQL 실행 실패"),
    SELECT_ALL_EXECUTION_FAILED("SELECT 전체 실행 실패"),
    STATISTICS_REFRESH_FAILED("통계 갱신 실패"),
    CLOSED_SQL_EXECUTOR("이미 닫힌 SQL 실행기입니다."),
    LVM_SNAPSHOT_ENVIRONMENT_CREATION_FAILED("LVM 스냅샷 실행 환경 생성 실패"),
    SEALED_TEMPLATE_NOT_REGISTERED("봉인된 템플릿이 등록되지 않았습니다: %s"),
    TEMPLATE_DBMS_MISMATCH("데이터셋 템플릿 DBMS 유형이 데이터셋과 다릅니다: %s"),
    UNKNOWN_DATASET_ID("알 수 없는 데이터셋 ID: %s"),
    UNKNOWN_SETUP_SQL_ID("알 수 없는 설정 SQL 묶음 ID: %s"),
    SETUP_SQL_DATASET_MISMATCH("설정 SQL 묶음의 대상 데이터셋이 다릅니다: %s"),
    DATASET_TEMPLATE_PREPARATION_FAILED("LVM 스냅샷 데이터셋 템플릿 준비 실패"),
    DATASET_TEMPLATE_TABLE_NOT_CREATED("데이터셋 템플릿에 생성된 테이블이 없습니다."),
    LVM_SNAPSHOT_NODE_CONFIG_NOT_FOUND("LVM 스냅샷 실행 노드 설정이 없습니다: %s"),
    REQUIRED_TEXT_BLANK("필수 문자열이 비어 있습니다."),
    REQUIRED_FIELD_BLANK("%s이 비어 있습니다."),
    RUNTIME_DB_LEASE_UNAVAILABLE("사용 가능한 런타임 DB 점유가 없습니다: %s"),
    RUNTIME_DB_CONFIG_NOT_READY("준비된 런타임 DB 설정이 없습니다: %s"),
    UNKNOWN_RUNTIME_DB_NODE("알 수 없는 런타임 DB 노드: %s"),
    RUNTIME_DB_NODE_NOT_READY("런타임 DB 노드가 준비되지 않았습니다: %s"),
    DEFINITION_SERIALIZE_FAILED("정의 직렬화에 실패했습니다."),
    DEFINITION_DESERIALIZE_FAILED("정의 역직렬화에 실패했습니다."),
    LVM_SNAPSHOT_PORT_UNAVAILABLE("사용 가능한 LVM 스냅샷 런타임 포트가 없습니다."),
    LVM_SNAPSHOT_WAIT_INTERRUPTED("LVM 스냅샷 작업 대기 중 중단"),
    LVM_COMMAND_TIMEOUT("LVM 런타임 명령 시간이 초과되었습니다: %s"),
    LVM_COMMAND_FAILED("LVM 런타임 명령 실패: %s"),
    LVM_COMMAND_FAILED_WITH_OUTPUT("LVM 런타임 명령 실패: %s%s%s"),
    LVM_COMMAND_INTERRUPTED("LVM 런타임 명령이 중단되었습니다: %s"),
    UNKNOWN_LVM_EVAL_SNAPSHOT_NAME("알 수 없는 LVM 평가 스냅샷 이름입니다: %s"),
    ANALYZE_API_REQUIRED("ANALYZE는 analyze API로 실행해야 합니다."),
    SQL_RESULT_SET_NOT_RETURNED("SQL이 결과 집합을 반환하지 않았습니다."),
    SQL_PLAN_NOT_RETURNED("SQL이 실행 계획을 반환하지 않았습니다."),
    LVM_DB_PROCESS_READY_FAILED("LVM 스냅샷 DB 프로세스 준비 실패"),
    LVM_ENVIRONMENT_CONNECTION_FAILED("LVM 스냅샷 실행 환경 연결 실패"),
    LVM_TEMPLATE_DB_PROCESS_READY_FAILED("LVM 스냅샷 템플릿 DB 프로세스 준비 실패"),
    LVM_TEMPLATE_DB_PROCESS_WAIT_INTERRUPTED("LVM 스냅샷 템플릿 DB 프로세스 대기 중 중단"),
    DOCKER_COMMAND_TIMEOUT("Docker 명령 시간이 초과되었습니다: %s"),
    DOCKER_COMMAND_FAILED("Docker 명령 실패: %s"),
    DOCKER_COMMAND_FAILED_WITH_OUTPUT("Docker 명령 실패: %s%s%s"),
    DOCKER_COMMAND_INTERRUPTED("Docker 명령이 중단되었습니다: %s");

    private final String message;

    JudgeFailReason(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        // 메시지 포맷 적용
        return message.formatted(args);
    }

}
