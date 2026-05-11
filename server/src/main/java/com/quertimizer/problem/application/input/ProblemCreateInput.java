package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemCreateProgress;
import lombok.Data;

import java.util.List;
import java.util.function.Consumer;

@Data
public class ProblemCreateInput {

    private final String title;
    private final String description;
    private final String condition;
    private final String output;
    private final String answerSql;
    private final String actualDataSql;
    private final String problemSetId;
    private final String problemId;
    private final DbmsType dbmsType;
    private final String ddl;
    private final String problemDdl;
    private final List<String> hiddenDataSqls;
    private final Consumer<ProblemCreateProgress> progressListener;

    public ProblemCreateInput(String title, String description, String condition, String output, String answerSql,
                              String actualDataSql, String problemSetId,
                              String problemId, String dbms, String ddl, String problemDdl,
                              List<String> hiddenDataSqls) {
        this(
                title, description, condition, output, answerSql,
                actualDataSql, problemSetId, problemId, dbms, ddl, problemDdl,
                hiddenDataSqls, null
        );
    }

    public ProblemCreateInput(String title, String description, String condition, String output, String answerSql,
                              String actualDataSql, String problemSetId,
                              String problemId, String dbms, String ddl, String problemDdl,
                              List<String> hiddenDataSqls,
                              Consumer<ProblemCreateProgress> progressListener) {
        this.title = normalize(title);
        this.description = normalize(description);
        this.condition = normalize(condition);
        this.output = normalize(output);
        this.answerSql = normalize(answerSql);
        this.actualDataSql = normalize(actualDataSql);
        this.problemSetId = normalize(problemSetId);
        this.problemId = normalize(problemId);
        this.dbmsType = DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
        this.ddl = normalize(ddl);
        String normalizedProblemDdl = normalize(problemDdl);
        this.problemDdl = normalizedProblemDdl.isBlank() ? this.ddl : normalizedProblemDdl;
        this.hiddenDataSqls = normalizeList(hiddenDataSqls);
        this.progressListener = progressListener;
    }

    private String normalize(String value) {
        // 입력 문자열 공백 제거와 null 정규화
        return value != null ? value.trim() : "";
    }

    private List<String> normalizeList(List<String> values) {
        // 입력 문자열 목록 공백 제거와 빈 값 제외
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
