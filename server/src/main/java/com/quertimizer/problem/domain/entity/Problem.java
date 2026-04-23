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
    private String outputSample;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answer_sql", columnDefinition = "TEXT")
    private String answerSql;

    public static Problem create(String problemId, String title, String description) {
        DbmsType dbmsType = problemId != null && problemId.startsWith("O") ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
        return new Problem(problemId, resolveProblemSetId(problemId), title, description, "", dbmsType == DbmsType.POSTGRESQL, dbmsType == DbmsType.ORACLE, "", "", "", "", "");
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
                                 String outputSample,
                                 String answer,
                                 String answerSql) {
        return new Problem(problemId, problemSetId, title, description, ddl, isPostgresql, isOracle, condition, output, outputSample, answer, answerSql);
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              boolean isPostgresql,
                              boolean isOracle,
                              String condition,
                              String output,
                              String outputSample,
                              String answer,
                              String answerSql) {
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.isPostgresql = isPostgresql;
        this.isOracle = isOracle;
        this.condition = condition;
        this.output = output;
        this.outputSample = outputSample;
        this.answer = answer;
        this.answerSql = answerSql;
    }

    public boolean supportsDbms(DbmsType dbmsType) {
        if (dbmsType == DbmsType.ORACLE) {
            return isOracle;
        }

        return isPostgresql;
    }

    public boolean hasSupportedDbms() {
        return isPostgresql || isOracle;
    }

    public DbmsType getDbmsType() {
        return isOracle ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    public String getResolvedProblemSetId() {
        if (problemSetId != null && !problemSetId.isBlank()) {
            return problemSetId;
        }

        return resolveProblemSetId(problemId);
    }

    public String getBaseProblemSetId() {
        return extractBaseProblemSetId(getResolvedProblemSetId());
    }

    private static String resolveProblemSetId(String problemId) {
        String[] tokens = problemId != null ? problemId.split("-") : new String[0];
        return tokens.length > 0 ? tokens[0] : "";
    }

    private static String extractBaseProblemSetId(String problemSetId) {
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
                    String outputSample,
                    String answer,
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
        this.outputSample = outputSample;
        this.answer = answer;
        this.answerSql = answerSql;
    }

}
