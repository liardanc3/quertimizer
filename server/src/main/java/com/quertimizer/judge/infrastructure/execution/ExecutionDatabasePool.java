package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutionDatabasePool {

    private final JudgeDatabaseCluster judgeDatabaseCluster;

    public List<JudgeDatabaseNode> getWorkers(DbmsType dbmsType) {
        // DBMS 유형별 judge DB node 목록을 조회
        return judgeDatabaseCluster.getReadyNodes(dbmsType);
    }
}
