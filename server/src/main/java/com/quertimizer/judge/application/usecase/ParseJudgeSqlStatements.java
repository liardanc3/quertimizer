package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.output.JudgeSqlStatement;
import com.quertimizer.judge.domain.policy.SqlExecutionPolicy;
import com.quertimizer.judge.infrastructure.runtime.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SQL 문자열을 judge 문장 단위로 분리하고 실행 모드를 분류한다.
 */
@Component
@RequiredArgsConstructor
public class ParseJudgeSqlStatements {

    private final SqlStatementParser statementParser;
    private final SqlExecutionPolicy executionPolicy;

    /**
     * SQL 문자열을 judge 문장 단위로 분리하고 실행 모드를 분류한다.
     *
     * @param sql 분리와 분류 대상 SQL 문자열
     * @return 분리된 SQL 문장 목록
     */
    public List<JudgeSqlStatement> execute(String sql) {
        return statementParser.splitStatements(sql).stream()
                .map(statementSql -> new JudgeSqlStatement(statementSql, executionPolicy.resolveMode(statementSql)))
                .toList();
    }
}
