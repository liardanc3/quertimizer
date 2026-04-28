package com.quertimizer.judge.application.port;

import java.sql.Connection;
import java.sql.SQLException;

public interface JudgeDatabaseLeasePort extends AutoCloseable {

    JudgeDatabaseNodePort node();

    Connection openConnection() throws SQLException;

    @Override
    void close();
}
