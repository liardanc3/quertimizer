package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;

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
        this.lease = Objects.requireNonNull(lease, "lease must not be null");
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
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
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }
}
