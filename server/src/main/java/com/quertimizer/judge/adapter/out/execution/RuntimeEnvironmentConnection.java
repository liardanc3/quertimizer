package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.application.service.JudgeDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class RuntimeEnvironmentConnection implements AutoCloseable {

    private final RuntimeDatabaseLease lease;
    private final Connection connection;
    private final JudgeDialect dialect;
    private final String environmentName;

    RuntimeEnvironmentConnection(RuntimeDatabaseLease lease, Connection connection,
                                 JudgeDialect dialect, String environmentName) {
        this.lease = Objects.requireNonNull(lease, "필수 값이 없다.");
        this.connection = Objects.requireNonNull(connection, "필수 값이 없다.");
        this.dialect = Objects.requireNonNull(dialect, "필수 값이 없다.");
        this.environmentName = requireText(environmentName, "environmentName");
    }

    public Connection getConnection() {
        return connection;
    }

    public JudgeDialect getDialect() {
        return dialect;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        } finally {
            lease.close();
        }
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있다.");
        }

        return value.trim();
    }
}
