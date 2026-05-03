package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

@Getter
public class ProblemCreateInput {

    private final String title;
    private final String description;
    private final String condition;
    private final String output;
    private final String answerSql;
    private final String sampleDataSql;
    private final String actualDataSql;
    private final String problemSetId;
    private final String problemId;
    private final DbmsType dbmsType;
    private final String ddl;

    public ProblemCreateInput(String title, String description, String condition, String output, String answerSql,
                              String sampleDataSql, String actualDataSql,
                              String problemSetId, String problemId, String dbms, String ddl) {
        this.title = normalize(title);
        this.description = normalize(description);
        this.condition = normalize(condition);
        this.output = normalize(output);
        this.answerSql = normalize(answerSql);
        this.sampleDataSql = normalize(sampleDataSql);
        this.actualDataSql = normalize(actualDataSql);
        this.problemSetId = normalize(problemSetId);
        this.problemId = normalize(problemId);
        this.dbmsType = DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
        this.ddl = normalize(ddl);
    }

    private String normalize(String value) {
        // 입력 문자열 공백 제거와 null 정규화
        return value != null ? value.trim() : "";
    }
}
