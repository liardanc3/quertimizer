package com.quertimizer.problem.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import lombok.Getter;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_UPDATE_DATA_INVALID;

@Getter
public class Problem {

    private Long id;
    private String problemId;
    private String problemSetId;
    private String title;
    private String description;
    private String ddl;
    private DbmsType dbmsType;
    private String condition;
    private String output;
    private String dataExample;
    private String outputExample;
    private String schemaMetadata;
    private String answerHash;
    private String answerSql;

    public static Problem create(String problemSetId) {
        // 문제 테이블셋 번호 기준 DBMS 유형을 가진 빈 문제 엔티티 생성
        String normalizedProblemSetId = requireProblemSetId(problemSetId);
        DbmsType dbmsType = DbmsType.fromScopedId(normalizedProblemSetId).orElse(DbmsType.POSTGRESQL);
        return new Problem(
                "", normalizedProblemSetId,
                "", "", "", dbmsType,
                "", "", "", "", "", "", ""
        );
    }

    public static Problem create(String problemSetId,
                                 String title,
                                 String description,
                                 String ddl,
                                 DbmsType dbmsType,
                                 String condition,
                                 String output,
                                 String dataExample,
                                 String outputExample,
                                 String schemaMetadata,
                                 String answerHash,
                                 String answerSql) {
        // 문제 테이블셋과 judge 산출물을 가진 신규 문제 엔티티 생성
        String normalizedProblemSetId = requireProblemSetId(problemSetId);
        return new Problem(
                "", normalizedProblemSetId,
                title, description, ddl, dbmsType,
                condition, output, dataExample, outputExample, schemaMetadata, answerHash, answerSql
        );
    }

    public static Problem create(String problemId, String title, String description, DbmsType dbmsType) {
        // 문제 생성
        return new Problem(problemId, resolveProblemSetId(problemId), title, description, "", dbmsType, "", "", "", "", "", "", "");
    }

    public static Problem create(String problemId,
                                 String problemSetId,
                                 String title,
                                 String description,
                                 String ddl,
                                 DbmsType dbmsType,
                                 String condition,
                                 String output,
                                 String dataExample,
                                 String outputExample,
                                 String schemaMetadata,
                                 String answerHash,
                                 String answerSql) {
        return new Problem(
                problemId, problemSetId, title, description, ddl, dbmsType,
                condition, output, dataExample, outputExample, schemaMetadata, answerHash, answerSql
        );
    }

    public static Problem restore(Long id, String problemId, String problemSetId,
                                  String title, String description, String ddl,
                                  DbmsType dbmsType, String condition,
                                  String output, String dataExample,
                                  String outputExample, String schemaMetadata,
                                  String answerHash,
                                  String answerSql) {
        // 저장된 문제 상태 복원
        Problem problem = new Problem(
                problemId, problemSetId, title, description, ddl, dbmsType,
                condition, output, dataExample, outputExample, schemaMetadata, answerHash, answerSql
        );
        problem.id = id;
        return problem;
    }

    public Problem updateInfo(String title,
                              String description,
                              String ddl,
                              DbmsType dbmsType,
                              String condition,
                              String output,
                              String answerSql) {
        // 요청 입력값으로 문제 기본 정보 전체 교체
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.answerSql = answerSql;
        return this;
    }

    public Problem updateInfo(String title,
                              String description,
                              String ddl,
                              DbmsType dbmsType,
                              String condition,
                              String output,
                              String dataExample,
                              String outputExample,
                              String schemaMetadata,
                              String answerHash,
                              String answerSql) {
        // 요청 입력값과 judge 산출물로 문제 정보 전체 교체
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.dataExample = dataExample;
        this.outputExample = outputExample;
        this.schemaMetadata = schemaMetadata;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
        return this;
    }

    public Problem update(String title, String description, String condition, String output) {
        // 문제 설명성 정보만 교체
        this.title = title;
        this.description = description;
        this.condition = condition;
        this.output = output;
        return this;
    }

    public Problem updateAnswerHash(String answerHash) {
        // 문제 정답 해시 교체
        this.answerHash = answerHash;
        return this;
    }

    public Problem validateSql() {
        // 문제 SQL 자료 유효성 검증
        validateText(ddl);
        validateText(answerSql);

        return this;
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              DbmsType dbmsType,
                              String condition,
                              String output,
                              String dataExample,
                              String outputExample,
                              String schemaMetadata,
                              String answerHash,
                              String answerSql) {
        // 요청 입력값과 judge 산출물로 문제 정보 전체 교체
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.dataExample = dataExample;
        this.outputExample = outputExample;
        this.schemaMetadata = schemaMetadata;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }

    public String getAnswer() {
        // 기존 정답 접근 코드를 위한 호환 getter
        return answerHash;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // 지원 DBMS 여부 확인
        return this.dbmsType == dbmsType;
    }

    public boolean hasSupportedDbms() {
        // 지원 DBMS 보유 여부 확인
        return dbmsType != null;
    }

    public DbmsType getDbmsType() {
        // DBMS 유형 반환
        return dbmsType;
    }

    public String getResolvedProblemSetId() {
        // 문제 테이블셋 번호 조회
        if (problemSetId != null && !problemSetId.isBlank()) {
            return problemSetId;
        }

        return resolveProblemSetId(problemId);
    }

    public String getBaseProblemSetId() {
        // 기준 문제 테이블셋 번호 조회
        return extractBaseProblemSetId(getResolvedProblemSetId());
    }

    private static String resolveProblemSetId(String problemId) {
        // 문제 테이블셋 번호 결정
        String[] tokens = problemId != null ? problemId.split("-") : new String[0];
        return tokens.length > 0 ? tokens[0] : "";
    }

    private static String extractBaseProblemSetId(String problemSetId) {
        // 기준 문제 테이블셋 번호 추출
        return DbmsType.extractBaseProblemSetId(problemSetId);
    }

    private static String requireProblemSetId(String problemSetId) {
        // 문제 테이블셋 번호 null 또는 공백 여부 검사
        if (problemSetId == null || problemSetId.isBlank()) {
            throw new IllegalArgumentException("problemSetId is required.");
        }

        // 문제 테이블셋 번호 공백 제거 후 반환
        return problemSetId.trim();
    }

    private void validateText(String value) {
        // 문제 업데이트 필수 문자열 존재 여부 검사
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException(PROBLEM_UPDATE_DATA_INVALID.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
        }
    }

    private Problem(String problemId,
                    String problemSetId,
                    String title,
                    String description,
                    String ddl,
                    DbmsType dbmsType,
                    String condition,
                    String output,
                    String dataExample,
                    String outputExample,
                    String schemaMetadata,
                    String answerHash,
                    String answerSql) {
        this.problemId = problemId;
        this.problemSetId = problemSetId;
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.dataExample = dataExample;
        this.outputExample = outputExample;
        this.schemaMetadata = schemaMetadata;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }

}
