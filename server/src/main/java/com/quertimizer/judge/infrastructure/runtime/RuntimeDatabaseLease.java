package com.quertimizer.judge.infrastructure.runtime;

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
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.releaseAction = Objects.requireNonNull(releaseAction, "releaseAction must not be null");
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
