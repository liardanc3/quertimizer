package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

@Getter
public class JudgeDatabaseNode {

    private final String id;
    private final String name;
    private final DbmsType engine;
    private final String url;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final int maxConcurrency;
    private final int weight;
    private final JudgeDatabaseConnectionProvider connectionProvider;
    private final Semaphore permits;

    public JudgeDatabaseNode(String id,
                             String name,
                             DbmsType engine,
                             String url,
                             String username,
                             String password,
                             boolean enabled,
                             int maxConcurrency,
                             int weight,
                             JudgeDatabaseConnectionProvider connectionProvider) {
        this.id = id;
        this.name = name;
        this.engine = engine;
        this.url = url;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.maxConcurrency = Math.max(1, maxConcurrency);
        this.weight = Math.max(1, weight);
        this.connectionProvider = connectionProvider;
        this.permits = new Semaphore(this.maxConcurrency, true);
    }

    public boolean isReady() {
        // 사용 가능한 judge DB node 여부 확인
        return enabled && !isBlank(url) && !isBlank(username);
    }

    public boolean tryAcquire() {
        // node 동시 실행 permit 점유
        return permits.tryAcquire();
    }

    public void release() {
        // node 동시 실행 permit 반환
        permits.release();
    }

    public int availablePermits() {
        // 남은 실행 permit 수 조회
        return permits.availablePermits();
    }

    public Connection openConnection() throws SQLException {
        // node 커넥션 생성
        return connectionProvider.openConnection(this);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
