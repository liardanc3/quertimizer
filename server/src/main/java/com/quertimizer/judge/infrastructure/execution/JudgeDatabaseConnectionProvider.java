package com.quertimizer.judge.infrastructure.execution;

import java.sql.Connection;
import java.sql.SQLException;

public interface JudgeDatabaseConnectionProvider {

    Connection openConnection(JudgeDatabaseNode node) throws SQLException;
}
