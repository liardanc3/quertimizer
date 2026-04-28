package com.quertimizer.judge.infrastructure.execution;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DriverManagerJudgeDatabaseConnectionProvider implements JudgeDatabaseConnectionProvider {

    @Override
    public Connection openConnection(JudgeDatabaseNode node) throws SQLException {
        // judge DB node 기준으로 커넥션을 생성
        return DriverManager.getConnection(node.getUrl(), node.getUsername(), node.getPassword());
    }
}
