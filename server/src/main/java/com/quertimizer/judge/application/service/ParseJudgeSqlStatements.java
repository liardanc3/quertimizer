package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.ParseJudgeSqlStatementsUseCase;
import com.quertimizer.judge.application.output.JudgeSqlStatement;
import com.quertimizer.judge.domain.policy.SqlExecutionPolicy;
import com.quertimizer.judge.application.service.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ParseJudgeSqlStatements implements ParseJudgeSqlStatementsUseCase {

    private final SqlStatementParser statementParser;
    private final SqlExecutionPolicy executionPolicy;

    /**
     * SQL 문자열을 judge 문장 단위로 분리하고 실행 모드를 분류한다.
     *
     * @param sql 분리와 분류 대상 SQL 문자열
     * @return 분리된 SQL 문장 목록
     */
    @Override
    public List<JudgeSqlStatement> execute(String sql) {
        return statementParser.splitStatements(sql).stream()
                .map(statementSql -> new JudgeSqlStatement(statementSql, executionPolicy.resolveMode(statementSql)))
                .toList();
    }
}
