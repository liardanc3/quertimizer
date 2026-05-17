package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.input.AnalyzeEnvironmentInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedSqlInput;
import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.application.model.ProvisionedEnvironment;
import com.quertimizer.judge.application.model.SqlExecutorTicket;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.out.EnvironmentProvisioner;
import com.quertimizer.judge.application.port.out.SqlExecutionPort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.ExecutionMode;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.judge.domain.model.JudgeFailReason.CLOSED_SQL_EXECUTOR;
import static com.quertimizer.judge.domain.model.JudgeFailReason.ISOLATED_SQL_EXECUTION_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SELECT_ALL_EXECUTION_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SETUP_SQL_EXECUTION_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SQL_EXECUTION_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.STATISTICS_REFRESH_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_ENVIRONMENT_ID;

@Slf4j
public class SqlExecutor implements AutoCloseable {

    private final SqlExecutorTicket ticket;
    private final ConcurrentHashMap<JudgeEnvironmentId, SqlExecutorPool.PersistentSqlExecutorEnvironment> environments;
    private final EnvironmentProvisioner environmentProvisioner;
    private final SqlExecutionPort sqlExecutionPort;
    private final DatasetTemplateService datasetTemplateService;
    private boolean closed;

    public SqlExecutor(SqlExecutorTicket ticket,
                       ConcurrentHashMap<JudgeEnvironmentId, SqlExecutorPool.PersistentSqlExecutorEnvironment> environments,
                       EnvironmentProvisioner environmentProvisioner, SqlExecutionPort sqlExecutionPort,
                       DatasetTemplateService datasetTemplateService) {
        this.ticket = ticket;
        this.environments = environments;
        this.environmentProvisioner = environmentProvisioner;
        this.sqlExecutionPort = sqlExecutionPort;
        this.datasetTemplateService = datasetTemplateService;
    }

    public DatasetTemplateDefinition createDataset(DatasetDefinition dataset) {
        // ticket 기준 데이터셋 템플릿 생성 위임
        requireOpen();
        return datasetTemplateService.createDatasetTemplate(dataset, ticket);
    }

    public JudgeEnvironmentId createEnvironment(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy) {
        // ticket 기준 실행 환경 생성과 영속 registry 등록
        requireOpen();
        ProvisionedEnvironment environment = environmentProvisioner.create(
                environmentId, dataset, policy, ticket.getPriority(), ticket.getStatusListener()
        );
        environments.put(environmentId, new SqlExecutorPool.PersistentSqlExecutorEnvironment(environment));
        return environmentId;
    }

    public SqlExecutionResult execute(ExecuteSqlInput command, String sql, ExecutionMode mode) {
        // 영속 실행 환경 점유 후 SQL 실행
        requireOpen();
        return withPersistentConnection(
                command.getEnvironmentId(), command.getOptions().getTimeoutSeconds(),
                connection -> execute(command, sql, mode, connection)
        );
    }

    public SqlExecutionResult executeSelectAll(ExecuteSqlInput command, String sql) {
        // 영속 실행 환경 점유 후 SELECT 전체 결과 실행
        requireOpen();
        return withPersistentConnection(
                command.getEnvironmentId(), command.getOptions().getTimeoutSeconds(),
                connection -> executeSelectAll(
                        command.getExecutionId(), connection, sql,
                        command.getOptions().isIncludeCost(), command.getOptions().isIncludePlan()
                )
        );
    }

    public SqlExecutionResult analyze(AnalyzeEnvironmentInput input) {
        // 영속 실행 환경 점유 후 DBMS 통계 갱신
        requireOpen();
        return withPersistentConnection(
                input.getEnvironmentId(), input.getOptions().getTimeoutSeconds(),
                connection -> analyze(input.getExecutionId(), connection)
        );
    }

    public SqlExecutionResult executeIsolated(ExecuteIsolatedSqlInput input, DatasetDefinition dataset,
                                              List<SetupSqlDefinition> setupSqlDefinitions) {
        // 격리 실행 환경 생성 후 설정 SQL, 통계 갱신, 대상 SQL 실행
        requireOpen();
        JudgeEnvironmentId environmentId = new JudgeEnvironmentId("environment-" + java.util.UUID.randomUUID());
        ProvisionedEnvironment environment = null;
        EnvironmentConnection connection = null;
        boolean completed = false;
        try {
            environment = environmentProvisioner.create(
                    environmentId, dataset,
                    new EnvironmentPolicy(input.getIsolationPolicy().isInitializeStatisticsAfterLoad(), true, false),
                    ticket.getPriority(), ticket.getStatusListener()
            );
            connection = environmentProvisioner.openConnection(environment, input.getOptions().getTimeoutSeconds());
            if (input.getIsolationPolicy().isApplySetupSqls()) {
                executeSetupSqls(connection, setupSqlDefinitions);
            }
            if (input.getIsolationPolicy().isInitializeStatisticsAfterSetup()) {
                analyze(input.getExecutionId(), connection);
            }
            SqlExecutionResult result = executeSelectAll(
                    input.getExecutionId(), connection, input.getTargetSql(),
                    input.getOptions().isIncludeCost(), input.getOptions().isIncludePlan()
            );
            completed = true;
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw new IllegalStateException(ISOLATED_SQL_EXECUTION_FAILED.getMessage(), exception);
        } finally {
            closeQuietly(connection);
            dropIsolatedEnvironment(environment, !completed || input.getIsolationPolicy().isDropEnvironmentAfterExecution());
        }
    }

    @Override
    public void close() {
        // 실행기 중복 종료 차단
        closed = true;
    }

    private SqlExecutionResult execute(ExecuteSqlInput command, String sql, ExecutionMode mode,
                                       EnvironmentConnection connection) {
        // 점유된 실행 환경에서 SQL 실행과 커밋 처리
        try {
            SqlExecutionResult result = sqlExecutionPort.execute(command, sql, mode, connection);
            connection.getConnection().commit();
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw new IllegalStateException(SQL_EXECUTION_FAILED.getMessage(), exception);
        }
    }

    private void executeSetupSqls(EnvironmentConnection connection,
                                  List<SetupSqlDefinition> setupSqlDefinitions) {
        // 점유된 실행 환경에 설정 SQL 목록 적용
        try {
            sqlExecutionPort.executeSetupSqls(connection, setupSqlDefinitions);
        } catch (Exception exception) {
            rollback(connection);
            throw new IllegalStateException(SETUP_SQL_EXECUTION_FAILED.getMessage(), exception);
        }
    }

    private SqlExecutionResult executeSelectAll(JudgeExecutionId executionId, EnvironmentConnection connection,
                                                String sql, boolean includeCost, boolean includePlan) {
        // 점유된 실행 환경에서 SELECT 전체 결과 실행
        try {
            SqlExecutionResult result = sqlExecutionPort.executeSelectAll(
                    executionId, connection, sql, includeCost, includePlan
            );
            connection.getConnection().commit();
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw new IllegalStateException(SELECT_ALL_EXECUTION_FAILED.getMessage(), exception);
        }
    }

    private SqlExecutionResult analyze(JudgeExecutionId executionId, EnvironmentConnection connection) {
        // 점유된 실행 환경의 DBMS 통계 갱신
        try {
            SqlExecutionResult result = sqlExecutionPort.executeAnalyze(executionId, connection);
            connection.getConnection().commit();
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw new IllegalStateException(STATISTICS_REFRESH_FAILED.getMessage(), exception);
        }
    }

    private SqlExecutionResult withPersistentConnection(JudgeEnvironmentId environmentId, int timeoutSeconds,
                                                        SqlExecutorConnectionAction action) {
        // 영속 실행 환경 lock 확보와 JDBC 연결 생성
        SqlExecutorPool.PersistentSqlExecutorEnvironment persistentEnvironment = requireEnvironment(environmentId);
        persistentEnvironment.lock.lock();
        EnvironmentConnection connection = null;
        try {
            persistentEnvironment.requireAvailable();
            connection = environmentProvisioner.openConnection(persistentEnvironment.environment, timeoutSeconds);
            return action.execute(connection);
        } finally {
            closeQuietly(connection);
            persistentEnvironment.lock.unlock();
        }
    }

    private SqlExecutorPool.PersistentSqlExecutorEnvironment requireEnvironment(JudgeEnvironmentId environmentId) {
        // 영속 실행 환경 조회
        SqlExecutorPool.PersistentSqlExecutorEnvironment environment = environments.get(environmentId);
        if (environment == null) {
            throw new IllegalArgumentException(UNKNOWN_ENVIRONMENT_ID.format(environmentId));
        }

        return environment;
    }

    private void dropIsolatedEnvironment(ProvisionedEnvironment environment, boolean dropEnvironment) {
        // 격리 실행 환경 제거 정책 적용
        if (environment == null || !dropEnvironment) {
            return;
        }

        try {
            environmentProvisioner.drop(environment);
        } catch (RuntimeException exception) {
            log.warn(
                    "[채점 환경] 격리 실행 환경 정리 실패 environment={}",
                    environment.getExecutionEnvironment().getEnvironmentId(), exception
            );
        }
    }

    private void rollback(EnvironmentConnection connection) {
        if (connection != null) {
            rollback(connection.getConnection());
        }
    }

    private void rollback(Connection connection) {
        // 실패 트랜잭션 롤백
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(EnvironmentConnection connection) {
        if (connection != null) {
            connection.close();
        }
    }

    private void requireOpen() {
        // 닫힌 SQL 실행기 재사용 차단
        if (closed) {
            throw new IllegalStateException(CLOSED_SQL_EXECUTOR.getMessage());
        }
    }

    @FunctionalInterface
    private interface SqlExecutorConnectionAction {
        SqlExecutionResult execute(EnvironmentConnection connection);
    }
}
