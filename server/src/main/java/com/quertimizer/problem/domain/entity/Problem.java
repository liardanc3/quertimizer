package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem {

    @Id
    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    @Column(name = "problem_set_id", nullable = false, length = 6)
    private String problemSetId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "is_postgresql", nullable = false)
    private boolean isPostgresql;

    @Column(name = "is_oracle", nullable = false)
    private boolean isOracle;

    @Column(columnDefinition = "TEXT")
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "output_sample", columnDefinition = "TEXT")
    private String sampleOutput;

    @Column(columnDefinition = "TEXT")
    private String answerHash;

    @Column(name = "answer_sql", columnDefinition = "TEXT")
    private String answerSql;

    @Column(name = "sample_data_sql", columnDefinition = "TEXT")
    private String sampleDataSql;

    public static Problem create(String problemId, String title, String description) {
        // 문제 생성
        DbmsType dbmsType = problemId != null && problemId.startsWith("O") ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
        return new Problem(problemId, resolveProblemSetId(problemId), title, description, "", dbmsType == DbmsType.POSTGRESQL, dbmsType == DbmsType.ORACLE, "", "", "", "", "", "");
    }

    public static Problem create(String problemId,
                                 String problemSetId,
                                 String title,
                                 String description,
                                 String ddl,
                                 boolean isPostgresql,
                                 boolean isOracle,
                                 String condition,
                                 String output,
                                 String sampleDataSql,
                                 String sampleOutput,
                                 String answerHash,
                                 String answerSql) {
        return new Problem(problemId, problemSetId, title, description, ddl, isPostgresql, isOracle, condition, output, sampleDataSql, sampleOutput, answerHash, answerSql);
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              boolean isPostgresql,
                              boolean isOracle,
                              String condition,
                              String output,
                              String sampleDataSql,
                              String sampleOutput,
                              String answerHash,
                              String answerSql) {
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.isPostgresql = isPostgresql;
        this.isOracle = isOracle;
        this.condition = condition;
        this.output = output;
        this.sampleDataSql = sampleDataSql;
        this.sampleOutput = sampleOutput;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }

    public String getOutputSample() {
        // 기존 outputSample 접근 코드를 위한 호환 getter
        return sampleOutput;
    }

    public String getAnswer() {
        // 기존 answer 접근 코드를 위한 호환 getter
        return answerHash;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        // 지원 DBMS 여부 확인
        if (dbmsType == DbmsType.ORACLE) {
            return isOracle;
        }

        return isPostgresql;
    }

    public boolean hasSupportedDbms() {
        // 지원 DBMS 보유 여부 확인
        return isPostgresql || isOracle;
    }

    public DbmsType getDbmsType() {
        // DBMS 유형 조회
        return isOracle ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
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
        if (problemSetId == null || problemSetId.isBlank()) {
            return "";
        }

        return problemSetId.matches("^[PO]\\d{5}$") ? problemSetId.substring(1) : problemSetId;
    }

    private Problem(String problemId,
                    String problemSetId,
                    String title,
                    String description,
                    String ddl,
                    boolean isPostgresql,
                    boolean isOracle,
                    String condition,
                    String output,
                    String sampleDataSql,
                    String sampleOutput,
                    String answerHash,
                    String answerSql) {
        this.problemId = problemId;
        this.problemSetId = problemSetId;
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.isPostgresql = isPostgresql;
        this.isOracle = isOracle;
        this.condition = condition;
        this.output = output;
        this.sampleDataSql = sampleDataSql;
        this.sampleOutput = sampleOutput;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
    }

}
