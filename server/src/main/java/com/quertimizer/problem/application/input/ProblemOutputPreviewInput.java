package com.quertimizer.problem.application.input;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

@Getter
public class ProblemOutputPreviewInput {

    private final DbmsType dbmsType;
    private final String ddl;
    private final String sampleDataSql;
    private final String answerSql;
    private final String requester;
    private final String clientIp;

    public ProblemOutputPreviewInput(String dbms, String ddl, String sampleDataSql,
                                     String answerSql, String requester, String clientIp) {
        this.dbmsType = DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
        this.ddl = normalize(ddl);
        this.sampleDataSql = normalize(sampleDataSql);
        this.answerSql = normalize(answerSql);
        this.requester = normalize(requester);
        this.clientIp = normalize(clientIp);
    }

    private String normalize(String value) {
        // 입력 문자열 공백 제거와 null 정규화
        return value != null ? value.trim() : "";
    }
}
