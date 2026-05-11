package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class ProblemExamplePreviewInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String problemDdl;
    private final String actualDataSql;
    private final String answerSql;
    private final String requester;
    private final String clientIp;

    public ProblemExamplePreviewInput(String dbms, String ddl, String problemDdl,
                                      String actualDataSql, String answerSql,
                                      String requester, String clientIp) {
        this.dbmsType = DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
        this.ddl = normalize(ddl);
        this.problemDdl = normalize(problemDdl);
        this.actualDataSql = normalize(actualDataSql);
        this.answerSql = normalize(answerSql);
        this.requester = normalize(requester);
        this.clientIp = normalize(clientIp);
    }

    private String normalize(String value) {
        // 입력 문자열 공백 제거와 null 정규화
        return value != null ? value.trim() : "";
    }
}
