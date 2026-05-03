package com.quertimizer.problem.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import lombok.Getter;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_UPDATE_DATA_INVALID;

@Getter
public class ProblemSet {

    private Long id;
    private String problemSetId;
    private String ddl;
    private String actualDataSql;
    private String datasetId;
    private DbmsType dbmsType;

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String actualDataSql,
                                    DbmsType dbmsType) {
        return create(problemSetId, ddl, actualDataSql, dbmsType, "");
    }

    public static ProblemSet create(String problemSetId,
                                    String ddl,
                                    String actualDataSql,
                                    DbmsType dbmsType,
                                    String judgeDatasetId) {
        return new ProblemSet(problemSetId, ddl, actualDataSql, dbmsType, judgeDatasetId);
    }

    public static ProblemSet create(String dbms) {
        // 요청 DBMS 문자열을 문제 테이블셋 DBMS 유형으로 정규화
        return create(DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL));
    }

    public static ProblemSet create(DbmsType dbmsType) {
        // DBMS 유형만 확정한 빈 문제 테이블셋 엔티티 생성
        DbmsType resolvedDbmsType = dbmsType != null ? dbmsType : DbmsType.POSTGRESQL;
        return new ProblemSet("", "", "", resolvedDbmsType, "");
    }

    public static ProblemSet create(String ddl, String actualDataSql, DbmsType dbmsType) {
        // SQL 자료와 DBMS 유형을 가진 신규 문제 테이블셋 엔티티 생성
        DbmsType resolvedDbmsType = dbmsType != null ? dbmsType : DbmsType.POSTGRESQL;
        return new ProblemSet("", ddl, actualDataSql, resolvedDbmsType, "");
    }

    public static ProblemSet create(String ddl, String actualDataSql, DbmsType dbmsType, String judgeDatasetId) {
        // SQL 자료와 judge 데이터셋 정보를 가진 신규 문제 테이블셋 엔티티 생성
        DbmsType resolvedDbmsType = dbmsType != null ? dbmsType : DbmsType.POSTGRESQL;
        return new ProblemSet("", ddl, actualDataSql, resolvedDbmsType, judgeDatasetId);
    }

    public static ProblemSet restore(Long id, String problemSetId,
                                     String ddl, String actualDataSql,
                                     String datasetId, DbmsType dbmsType) {
        // 저장된 문제 테이블셋 상태 복원
        ProblemSet problemSet = new ProblemSet(problemSetId, ddl, actualDataSql, dbmsType, datasetId);
        problemSet.id = id;
        return problemSet;
    }

    public ProblemSet updateInfo(String ddl, String actualDataSql, DbmsType dbmsType) {
        // 문제 테이블셋 기본 SQL 자료 전체 교체
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.dbmsType = dbmsType;
        return this;
    }

    public ProblemSet updateInfo(String ddl, String actualDataSql, DbmsType dbmsType, String judgeDatasetId) {
        // 문제 테이블셋 SQL 자료와 judge 데이터셋 정보 전체 교체
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.dbmsType = dbmsType;
        this.datasetId = judgeDatasetId;
        return this;
    }

    public ProblemSet updateDatasetId(String datasetId) {
        // 문제 테이블셋 데이터셋 ID 교체
        this.datasetId = datasetId;
        return this;
    }

    public ProblemSet validateSql() {
        // 문제 테이블셋 SQL 자료 유효성 검증
        validateText(ddl);
        validateText(actualDataSql);
        return this;
    }

    public void changeContent(String ddl,
                              String actualDataSql,
                              DbmsType dbmsType,
                              String judgeDatasetId) {
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.dbmsType = dbmsType;
        this.datasetId = judgeDatasetId;
    }

    public String getData() {
        // 기존 data 접근 코드를 위한 호환 getter
        return actualDataSql;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // 이 테이블셋의 요청 DBMS 사용 가능 여부 판단
        return this.dbmsType == dbmsType;
    }

    public boolean hasSupportedDbms() {
        // DBMS가 지정된 최신 테이블셋 여부 판단
        return dbmsType != null;
    }

    public DbmsType getDbmsType() {
        // 이 테이블셋이 속한 DBMS 유형 반환
        return dbmsType;
    }

    public String getBaseProblemSetId() {
        // 기준 문제 테이블셋 번호 조회
        return DbmsType.extractBaseProblemSetId(problemSetId);
    }

    public void assignProblemSetId(Long id) {
        // DB 생성 ID 기반 문제 테이블셋 번호 부여 대상 여부 검사
        if (problemSetId != null && !problemSetId.isBlank()) {
            return;
        }

        // DBMS prefix와 DB 생성 ID 조합
        this.id = id;
        problemSetId = dbmsType.getIdPrefix() + formatFiveDigits(id);
    }

    private static String formatFiveDigits(Long value) {
        // 다섯 자리 문자열 포맷
        return "%05d".formatted(value);
    }

    private void validateText(String value) {
        // 문제 업데이트 필수 문자열 존재 여부 검사
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException(PROBLEM_UPDATE_DATA_INVALID.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
        }
    }

    private ProblemSet(String problemSetId,
                       String ddl,
                       String actualDataSql,
                       DbmsType dbmsType,
                       String datasetId) {
        this.problemSetId = problemSetId;
        this.ddl = ddl;
        this.actualDataSql = actualDataSql;
        this.dbmsType = dbmsType;
        this.datasetId = datasetId;
    }

}
