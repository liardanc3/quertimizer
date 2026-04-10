package com.quertimizer.entity;

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
    @Column(name = "problem_id", nullable = false, length = 11)
    private String problemId;

    @Column(name = "problem_set_id", nullable = false, length = 5)
    private String problemSetId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ddl_postgresql", columnDefinition = "TEXT")
    private String ddlPostgresql;

    @Column(name = "ddl_oracle", columnDefinition = "TEXT")
    private String ddlOracle;

    @Column(columnDefinition = "TEXT")
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "output_sample", columnDefinition = "TEXT")
    private String outputSample;

    @Column(columnDefinition = "TEXT")
    private String answer;

    public static Problem create(String problemId, String title, String description) {
        return new Problem(problemId, problemId.split("-")[0], title, description, null, null, null, null, null, null);
    }

    public static Problem create(String problemId,
                                 String problemSetId,
                                 String title,
                                 String description,
                                 String ddlPostgresql,
                                 String ddlOracle,
                                 String condition,
                                 String output,
                                 String outputSample,
                                 String answer) {
        return new Problem(problemId, problemSetId, title, description, ddlPostgresql, ddlOracle, condition, output, outputSample, answer);
    }

    public void changeContent(String title,
                              String description,
                              String ddlPostgresql,
                              String ddlOracle,
                              String condition,
                              String output,
                              String outputSample,
                              String answer) {
        this.title = title;
        this.description = description;
        this.ddlPostgresql = ddlPostgresql;
        this.ddlOracle = ddlOracle;
        this.condition = condition;
        this.output = output;
        this.outputSample = outputSample;
        this.answer = answer;
    }

    public String getResolvedProblemSetId() {
        if (problemSetId != null && !problemSetId.isBlank()) {
            return problemSetId;
        }

        String[] tokens = problemId.split("-");
        return tokens.length > 0 ? tokens[0] : "";
    }

    private Problem(String problemId,
                    String problemSetId,
                    String title,
                    String description,
                    String ddlPostgresql,
                    String ddlOracle,
                    String condition,
                    String output,
                    String outputSample,
                    String answer) {
        this.problemId = problemId;
        this.problemSetId = problemSetId;
        this.title = title;
        this.description = description;
        this.ddlPostgresql = ddlPostgresql;
        this.ddlOracle = ddlOracle;
        this.condition = condition;
        this.output = output;
        this.outputSample = outputSample;
        this.answer = answer;
    }

}
