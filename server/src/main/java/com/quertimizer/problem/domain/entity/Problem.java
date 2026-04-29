package com.quertimizer.problem.domain.entity;

import com.quertimizer.global.constant.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

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

    @Column(name = "sample_dataset_id", length = 80)
    private String sampleDatasetId;

    @Column(name = "judge_reference_id", length = 80)
    private String judgeReferenceId;

    public static Problem create(String problemId, String title, String description, DbmsType dbmsType) {
        // 문제 생성
        return new Problem(problemId, resolveProblemSetId(problemId), title, description, "", dbmsType, "", "", "", "", "", "", "", "");
    }

    public static Problem create(String problemId,
                                 String problemSetId,
                                 String title,
                                 String description,
                                 String ddl,
                                 DbmsType dbmsType,
                                 String condition,
                                 String output,
                                 String sampleDataSql,
                                 String sampleOutput,
                                 String answerHash,
                                 String answerSql) {
        return create(
                problemId, problemSetId, title, description, ddl, dbmsType,
                condition, output, sampleDataSql, sampleOutput, answerHash, answerSql, "", ""
        );
    }

    public static Problem create(String problemId,
                                 String problemSetId,
                                 String title,
                                 String description,
                                 String ddl,
                                 DbmsType dbmsType,
                                 String condition,
                                 String output,
                                 String sampleDataSql,
                                 String sampleOutput,
                                 String answerHash,
                                 String answerSql,
                                 String sampleDatasetId,
                                 String judgeReferenceId) {
        return new Problem(
                problemId, problemSetId, title, description, ddl, dbmsType,
                condition, output, sampleDataSql, sampleOutput, answerHash, answerSql, sampleDatasetId, judgeReferenceId
        );
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              DbmsType dbmsType,
                              String condition,
                              String output,
                              String sampleDataSql,
                              String sampleOutput,
                              String answerHash,
                              String answerSql) {
        changeContent(title, description, ddl, dbmsType, condition, output, sampleDataSql, sampleOutput, answerHash, answerSql, sampleDatasetId, judgeReferenceId);
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              DbmsType dbmsType,
                              String condition,
                              String output,
                              String sampleDataSql,
                              String sampleOutput,
                              String answerHash,
                              String answerSql,
                              String sampleDatasetId,
                              String judgeReferenceId) {
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.sampleDataSql = sampleDataSql;
        this.sampleOutput = sampleOutput;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
        this.sampleDatasetId = sampleDatasetId;
        this.judgeReferenceId = judgeReferenceId;
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
        return this.dbmsType == dbmsType;
    }

    public boolean hasSupportedDbms() {
        // 지원 DBMS 보유 여부 확인
        return dbmsType != null;
    }

    public DbmsType getDbmsType() {
        // DBMS 유형을 반환한다
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

    private Problem(String problemId,
                    String problemSetId,
                    String title,
                    String description,
                    String ddl,
                    DbmsType dbmsType,
                    String condition,
                    String output,
                    String sampleDataSql,
                    String sampleOutput,
                    String answerHash,
                    String answerSql,
                    String sampleDatasetId,
                    String judgeReferenceId) {
        this.problemId = problemId;
        this.problemSetId = problemSetId;
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.dbmsType = dbmsType;
        this.condition = condition;
        this.output = output;
        this.sampleDataSql = sampleDataSql;
        this.sampleOutput = sampleOutput;
        this.answerHash = answerHash;
        this.answerSql = answerSql;
        this.sampleDatasetId = sampleDatasetId;
        this.judgeReferenceId = judgeReferenceId;
    }

}
