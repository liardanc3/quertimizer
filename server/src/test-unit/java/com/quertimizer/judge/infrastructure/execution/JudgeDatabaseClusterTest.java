package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeDatabaseClusterTest {

    @Test
    @DisplayName("engine별 enabled node만 선택한다")
    void acquireOnlyEnabledReadyNodeByEngine() {
        // given
        JudgeDatabaseCluster cluster = createCluster(List.of(
                createDatabase("disabled-mysql", DbmsType.MYSQL, false, "jdbc:mysql://localhost:3306/disabled", "mysql", 1),
                createDatabase("blank-mysql", DbmsType.MYSQL, true, "", "mysql", 1),
                createDatabase("mysql-worker-1", DbmsType.MYSQL, true, "jdbc:mysql://localhost:3306/quertimizer", "mysql", 1),
                createDatabase("pg-worker-1", DbmsType.POSTGRESQL, true, "jdbc:postgresql://localhost:5432/postgres", "postgres", 1)
        ));

        // when
        JudgeDatabaseLease mysqlLease = cluster.acquire(DbmsType.MYSQL);

        // then
        assertEquals("mysql-worker-1", mysqlLease.node().getId());
        mysqlLease.close();
    }

    @Test
    @DisplayName("maxConcurrency 기준으로 node lease 점유를 제한한다")
    void respectMaxConcurrency() throws Exception {
        // given
        JudgeDatabaseCluster cluster = createCluster(List.of(
                createDatabase("mysql-worker-1", DbmsType.MYSQL, true, "jdbc:mysql://localhost:3306/quertimizer", "mysql", 1)
        ));
        JudgeDatabaseLease firstLease = cluster.acquire(DbmsType.MYSQL);
        CountDownLatch waitingLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // when
        Future<JudgeDatabaseLease> secondLeaseFuture = executorService.submit(() -> {
            waitingLatch.countDown();
            return cluster.acquire(DbmsType.MYSQL);
        });
        assertTrue(waitingLatch.await(1, TimeUnit.SECONDS));
        assertFalse(secondLeaseFuture.isDone());

        firstLease.close();
        JudgeDatabaseLease secondLease = secondLeaseFuture.get(1, TimeUnit.SECONDS);

        // then
        assertEquals("mysql-worker-1", secondLease.node().getId());
        secondLease.close();
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("release 후 같은 engine의 node를 다시 점유할 수 있다")
    void acquireAgainAfterRelease() {
        // given
        JudgeDatabaseCluster cluster = createCluster(List.of(
                createDatabase("pg-worker-1", DbmsType.POSTGRESQL, true, "jdbc:postgresql://localhost:5432/postgres", "postgres", 1)
        ));

        // when
        JudgeDatabaseLease firstLease = cluster.acquire(DbmsType.POSTGRESQL);
        firstLease.close();
        JudgeDatabaseLease secondLease = cluster.acquire(DbmsType.POSTGRESQL);

        // then
        assertEquals("pg-worker-1", secondLease.node().getId());
        secondLease.close();
    }

    @Test
    @DisplayName("사용 가능한 node가 없으면 명확한 예외가 발생한다")
    void throwClearExceptionWhenNoNodeExists() {
        // given
        JudgeDatabaseCluster cluster = createCluster(List.of(
                createDatabase("disabled-mysql", DbmsType.MYSQL, false, "jdbc:mysql://localhost:3306/quertimizer", "mysql", 1)
        ));

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cluster.acquire(DbmsType.MYSQL));
        assertEquals("mysql judge DB node 설정이 0개다.", exception.getMessage());
    }

    private JudgeDatabaseCluster createCluster(List<JudgeDatabaseProperties.DatabaseProperties> databases) {
        JudgeDatabaseProperties properties = new JudgeDatabaseProperties();
        properties.setDatabases(databases);
        JudgeDatabaseConnectionProvider connectionProvider = new JudgeDatabaseConnectionProvider() {

            @Override
            public Connection openConnection(JudgeDatabaseNode node) throws SQLException {
                throw new SQLException("테스트에서는 커넥션을 열지 않는다.");
            }
        };

        return new JudgeDatabaseCluster(properties, connectionProvider, new RoundRobinJudgeDatabaseSelector());
    }

    private JudgeDatabaseProperties.DatabaseProperties createDatabase(String id,
                                                                      DbmsType engine,
                                                                      boolean enabled,
                                                                      String url,
                                                                      String username,
                                                                      int maxConcurrency) {
        JudgeDatabaseProperties.DatabaseProperties properties = new JudgeDatabaseProperties.DatabaseProperties();
        properties.setId(id);
        properties.setName(id);
        properties.setEngine(engine.getValue());
        properties.setUrl(url);
        properties.setUsername(username);
        properties.setPassword("");
        properties.setEnabled(enabled);
        properties.setMaxConcurrency(maxConcurrency);
        return properties;
    }
}
