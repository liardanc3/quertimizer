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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String ddl;

    @Column(columnDefinition = "TEXT")
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "data_sample", columnDefinition = "TEXT")
    private String dataSample;

    @Column(name = "output_sample", columnDefinition = "TEXT")
    private String outputSample;

    @Column(columnDefinition = "TEXT")
    private String answer;

    public static Problem create(String problemId, String title, String description) {
        return new Problem(problemId, title, description, null, null, null, null, null, null);
    }

    public static Problem create(String problemId,
                                 String title,
                                 String description,
                                 String ddl,
                                 String condition,
                                 String output,
                                 String dataSample,
                                 String outputSample,
                                 String answer) {
        return new Problem(problemId, title, description, ddl, condition, output, dataSample, outputSample, answer);
    }

    public void changeContent(String title,
                              String description,
                              String ddl,
                              String condition,
                              String output,
                              String dataSample,
                              String outputSample,
                              String answer) {
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.condition = condition;
        this.output = output;
        this.dataSample = dataSample;
        this.outputSample = outputSample;
        this.answer = answer;
    }

    private Problem(String problemId,
                    String title,
                    String description,
                    String ddl,
                    String condition,
                    String output,
                    String dataSample,
                    String outputSample,
                    String answer) {
        this.problemId = problemId;
        this.title = title;
        this.description = description;
        this.ddl = ddl;
        this.condition = condition;
        this.output = output;
        this.dataSample = dataSample;
        this.outputSample = outputSample;
        this.answer = answer;
    }

}
