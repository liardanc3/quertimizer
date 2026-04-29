package com.quertimizer.sqljudge.runtime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Represents a leased runtime database node.
 */
public class RuntimeDatabaseLease implements AutoCloseable {

    private final RuntimeDatabase database;
    private final Runnable releaseAction;
    private boolean closed;

    /**
     * Creates a runtime database lease with no external release action.
     *
     * @param database leased runtime database
     */
    public RuntimeDatabaseLease(RuntimeDatabase database) {
        this(database, () -> {
        });
    }

    RuntimeDatabaseLease(RuntimeDatabase database, Runnable releaseAction) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.releaseAction = Objects.requireNonNull(releaseAction, "releaseAction must not be null");
    }

    /**
     * Returns the leased runtime database.
     *
     * @return leased runtime database
     */
    public RuntimeDatabase getDatabase() {
        return database;
    }

    /**
     * Opens a JDBC connection to the leased runtime database.
     *
     * @return JDBC connection
     * @throws SQLException when the JDBC connection cannot be opened
     */
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    /**
     * Releases this runtime database lease.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        releaseAction.run();
    }
}
