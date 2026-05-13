package com.quertimizer.judge.application.model;

import lombok.Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Data
public class DatabaseLease implements AutoCloseable {

    private final Database database;
    private final Runnable releaseAction;
    private boolean closed;

    public DatabaseLease(Database database) {
        this(database, () -> {
        });
    }

    public DatabaseLease(Database database, Runnable releaseAction) {
        this.database = database;
        this.releaseAction = releaseAction;
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    @Override
    public void close() {
        // 이미 반환된 lease 중복 반환 차단
        if (closed) {
            return;
        }

        // lease 닫힘 표시 후 반환 액션 실행
        closed = true;
        releaseAction.run();
    }
}
