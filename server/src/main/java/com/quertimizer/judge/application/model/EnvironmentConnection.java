package com.quertimizer.judge.application.model;

import com.quertimizer.judge.application.port.out.SqlDialect;
import lombok.Data;

import java.sql.Connection;
import java.sql.SQLException;

@Data
public class EnvironmentConnection implements AutoCloseable {

    private final DatabaseLease lease;
    private final Connection connection;
    private final SqlDialect dialect;
    private final String environmentName;

    public EnvironmentConnection(DatabaseLease lease, Connection connection,
                                        SqlDialect dialect, String environmentName) {
        this.lease = lease;
        this.connection = connection;
        this.dialect = dialect;
        this.environmentName = environmentName.trim();
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

}
