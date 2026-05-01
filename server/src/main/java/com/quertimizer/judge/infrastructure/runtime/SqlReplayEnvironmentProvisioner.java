package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialectProvider;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;

public class SqlReplayEnvironmentProvisioner implements RuntimeEnvironmentProvisioner {

    private final RuntimeDatabaseCluster databaseCluster;
    private final JudgeDialectProvider dialectProvider;
    private final RuntimeEnvironmentNamingStrategy namingStrategy;
    private final SqlStatementParser statementParser;
    private final RuntimeStatisticsInitializer statisticsInitializer;

    public SqlReplayEnvironmentProvisioner(RuntimeDatabaseCluster databaseCluster, JudgeDialectProvider dialectProvider,
                                           RuntimeEnvironmentNamingStrategy namingStrategy, SqlStatementParser statementParser) {
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "databaseCluster must not be null");
        this.dialectProvider = Objects.requireNonNull(dialectProvider, "dialectProvider must not be null");
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy must not be null");
        this.statementParser = Objects.requireNonNull(statementParser, "statementParser must not be null");
        this.statisticsInitializer = new RuntimeStatisticsInitializer();
    }

    @Override
    public ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        try (RuntimeDatabaseLease lease = databaseCluster.acquire(dataset.getDbmsType());
             Connection connection = lease.openConnection()) {
            RuntimeEnvironment environment = createRuntimeEnvironment(environmentId, dataset, lease.getDatabase());
            JudgeDialect dialect = dialectProvider.get(dataset.getDbmsType());

            connection.setAutoCommit(false);
            try {
                createEnvironment(connection, dialect, environment.getName().getValue());
                loadDataset(connection, dialect, environment.getName().getValue(), dataset,
                        policy.isApplyBaseIndexes(), policy.isInitializeStatisticsAfterLoad());
                connection.commit();
                return new ProvisionedRuntimeEnvironment(environment, provisionerName());
            } catch (Exception exception) {
                rollback(connection);
                cleanupEnvironment(connection, dialect, environment.getName().getValue());
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("sql-replay runtime environment creation failed", exception);
        }
    }

    @Override
    public RuntimeEnvironmentConnection openConnection(ProvisionedRuntimeEnvironment environment, int timeoutSeconds) {
        Objects.requireNonNull(environment, "environment must not be null");

        RuntimeEnvironment runtimeEnvironment = environment.getRuntimeEnvironment();
        RuntimeDatabaseLease lease = null;
        Connection connection = null;
        try {
            lease = databaseCluster.acquireNode(runtimeEnvironment.getDatabase().getId());
            connection = lease.openConnection();
            connection.setAutoCommit(false);

            JudgeDialect dialect = dialectProvider.get(runtimeEnvironment.getDatabase().getDbmsType());
            configureExecutionConnection(connection, dialect, runtimeEnvironment.getName().getValue(), timeoutSeconds);
            return new RuntimeEnvironmentConnection(lease, connection, dialect, runtimeEnvironment.getName().getValue());
        } catch (Exception exception) {
            closeQuietly(connection);
            closeQuietly(lease);
            throw new IllegalStateException("sql-replay runtime environment connection failed", exception);
        }
    }

    @Override
    public void drop(ProvisionedRuntimeEnvironment environment) {
        Objects.requireNonNull(environment, "environment must not be null");

        RuntimeEnvironment runtimeEnvironment = environment.getRuntimeEnvironment();
        try (RuntimeDatabaseLease lease = databaseCluster.acquireNode(runtimeEnvironment.getDatabase().getId());
             Connection connection = lease.openConnection()) {
            JudgeDialect dialect = dialectProvider.get(runtimeEnvironment.getDatabase().getDbmsType());
            dropEnvironment(connection, dialect, runtimeEnvironment.getName().getValue());
        } catch (Exception exception) {
            throw new IllegalStateException("sql-replay runtime environment drop failed", exception);
        }
    }

    private RuntimeEnvironment createRuntimeEnvironment(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                        RuntimeDatabase database) {
        return new RuntimeEnvironment(
                environmentId, dataset.getDatasetId(),
                database,
                namingStrategy.createName(environmentId, dataset.getDatasetId()),
                Instant.now()
        );
    }

    private void createEnvironment(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
            statement.execute(dialect.createEnvironmentSql(environmentName));
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
        }
    }

    private void loadDataset(Connection connection, JudgeDialect dialect,
                             String environmentName, DatasetDefinition dataset,
                             boolean applyBaseIndexes,
                             boolean initializeStatistics) throws Exception {
        executeStatements(connection, dataset.getDdl());
        executeStatements(connection, dataset.getDataSql());
        if (applyBaseIndexes) {
            for (String baseIndexDdl : dataset.getBaseIndexDdls()) {
                executeStatements(connection, baseIndexDdl);
            }
        }
        if (initializeStatistics) {
            statisticsInitializer.initialize(connection, dialect, environmentName);
        }
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private void configureExecutionConnection(Connection connection, JudgeDialect dialect,
                                              String environmentName,
                                              int timeoutSeconds) throws Exception {
        try (Statement statement = connection.createStatement()) {
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(timeoutSeconds)) {
                statement.execute(timeoutSql);
            }
        }
    }

    private void cleanupEnvironment(Connection connection, JudgeDialect dialect, String environmentName) {
        try {
            dropEnvironment(connection, dialect, environmentName);
        } catch (Exception ignored) {
        }
    }

    private void dropEnvironment(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(RuntimeDatabaseLease lease) {
        if (lease != null) {
            lease.close();
        }
    }

    private String provisionerName() {
        return "sql-replay";
    }
}
