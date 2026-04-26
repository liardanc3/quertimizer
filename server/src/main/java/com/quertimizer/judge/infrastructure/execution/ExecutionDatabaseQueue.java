package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutionDatabaseQueue {

    private final ExecutionDatabasePool executionDatabasePool;
    private final ExecutionDatabaseSelector executionDatabaseSelector;

    public synchronized ExecutionDatabasePool.ExecutionDatabaseWorker acquire(DbmsType dbmsType) {
        // 사용 가능한 execution worker를 대기 후 점유
        List<ExecutionDatabasePool.ExecutionDatabaseWorker> workers = executionDatabasePool.getWorkers(dbmsType);
        if (workers.isEmpty()) {
            throw new IllegalStateException("%s execution DB 설정이 0개다.".formatted(dbmsType.getValue()));
        }

        while (true) {
            int startIndex = executionDatabaseSelector.selectStartIndex(workers);
            for (int offset = 0; offset < workers.size(); offset++) {
                ExecutionDatabasePool.ExecutionDatabaseWorker worker = workers.get((startIndex + offset) % workers.size());
                if (!worker.isBusy()) {
                    worker.markBusy();
                    return worker;
                }
            }

            try {
                wait(200L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("execution DB 대기 중 인터럽트가 발생했다.", exception);
            }
        }
    }

    public synchronized void release(ExecutionDatabasePool.ExecutionDatabaseWorker worker) {
        // 점유한 execution worker를 반환
        worker.release();
        notifyAll();
    }
}
