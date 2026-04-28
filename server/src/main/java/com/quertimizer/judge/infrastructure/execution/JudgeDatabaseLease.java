package com.quertimizer.judge.infrastructure.execution;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class JudgeDatabaseLease implements AutoCloseable {

    private final JudgeDatabaseNode node;
    private final Runnable releaseAction;
    private final AtomicBoolean closed = new AtomicBoolean();

    JudgeDatabaseLease(JudgeDatabaseNode node, Runnable releaseAction) {
        this.node = node;
        this.releaseAction = releaseAction;
    }

    public JudgeDatabaseNode node() {
        // 점유한 judge DB node 조회
        return node;
    }

    public Connection openConnection() throws SQLException {
        // 점유한 node 커넥션 생성
        return node.openConnection();
    }

    @Override
    public void close() {
        // lease 반환
        if (closed.compareAndSet(false, true)) {
            releaseAction.run();
        }
    }
}
