package com.quertimizer.judge.adapter.out.execution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class RuntimeDatabaseLease implements AutoCloseable {

    private final RuntimeDatabase database;
    private final Runnable releaseAction;
    private boolean closed;

    public RuntimeDatabaseLease(RuntimeDatabase database) {
        this(database, () -> {
        });
    }

    RuntimeDatabaseLease(RuntimeDatabase database, Runnable releaseAction) {
        this.database = Objects.requireNonNull(database, "필수 값이 없다.");
        this.releaseAction = Objects.requireNonNull(releaseAction, "필수 값이 없다.");
    }

    public RuntimeDatabase getDatabase() {
        return database;
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        releaseAction.run();
    }
}
