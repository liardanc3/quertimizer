package com.quertimizer.judge.domain.model;

import lombok.Getter;

@Getter
public enum SqlPolicyFailReason {

    SQL_REQUIRED("SQL이 필요합니다."),
    DDL_REQUIRED("DDL이 필요합니다."),
    DATA_SQL_REQUIRED("데이터 SQL이 필요합니다."),
    SETUP_SQL_REQUIRED("설정 SQL은 비어 있을 수 없습니다."),
    SQL_LENGTH_EXCEEDED("SQL 길이가 제한을 초과했습니다."),
    SINGLE_SQL_ONLY("SQL 문장은 하나만 허용됩니다."),
    READ_ONLY_SINGLE_SQL_ONLY("읽기 전용 SQL 문장은 하나만 허용됩니다."),
    WRITE_CTE_UNSUPPORTED("쓰기 CTE 문장은 지원하지 않습니다."),
    UNSUPPORTED_SQL_COMMAND("지원하지 않는 SQL 명령입니다."),
    DANGEROUS_SQL_INCLUDED("SQL에 허용되지 않는 문장이 포함되어 있습니다."),
    DATASET_DDL_COMMAND_UNSUPPORTED("데이터셋 DDL에는 테이블, 코멘트, 인덱스 문장만 사용할 수 있습니다."),
    SETUP_SQL_COMMAND_UNSUPPORTED("설정 SQL에는 인덱스 또는 테이블 변경 문장만 사용할 수 있습니다."),
    READ_ONLY_SQL_REQUIRED("SELECT 또는 읽기 전용 WITH SQL만 허용됩니다."),
    DATASET_DATA_SQL_COMMAND_UNSUPPORTED("데이터셋 데이터 SQL에는 INSERT 문장만 사용할 수 있습니다."),
    POSITIVE_VALUE_REQUIRED("값은 0보다 커야 합니다.");

    private final String message;

    SqlPolicyFailReason(String message) {
        this.message = message;
    }

}
