package com.quertimizer.judge.infrastructure.execution;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinExecutionDatabaseSelector implements ExecutionDatabaseSelector {

    private final AtomicInteger nextIndex = new AtomicInteger();

    @Override
    public int selectStartIndex(List<ExecutionDatabasePool.ExecutionDatabaseWorker> workers) {
        // execution worker 목록에서 round-robin 시작 index를 결정
        if (workers.isEmpty()) {
            return 0;
        }

        return Math.floorMod(nextIndex.getAndIncrement(), workers.size());
    }
}
