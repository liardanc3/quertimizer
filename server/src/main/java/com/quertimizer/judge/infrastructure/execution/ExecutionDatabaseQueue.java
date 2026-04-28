package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionDatabaseQueue {

    private final JudgeDatabaseCluster judgeDatabaseCluster;

    public JudgeDatabaseLease acquire(DbmsType dbmsType) {
        // 사용 가능한 judge DB node lease를 점유
        return judgeDatabaseCluster.acquire(dbmsType);
    }

    public void release(JudgeDatabaseLease lease) {
        // 점유한 judge DB node lease를 반환
        judgeDatabaseCluster.release(lease);
    }
}
