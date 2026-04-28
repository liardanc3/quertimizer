package com.quertimizer.judge.infrastructure.execution;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinJudgeDatabaseSelector {

    private final AtomicInteger nextIndex = new AtomicInteger();

    public int selectStartIndex(List<JudgeDatabaseNode> nodes) {
        // judge DB node 목록에서 round-robin 시작 index를 결정
        if (nodes.isEmpty()) {
            return 0;
        }

        return Math.floorMod(nextIndex.getAndIncrement(), nodes.size());
    }
}
