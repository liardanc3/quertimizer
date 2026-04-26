package com.quertimizer.judge.infrastructure.execution;

import java.util.List;

public interface ExecutionDatabaseSelector {

    int selectStartIndex(List<ExecutionDatabasePool.ExecutionDatabaseWorker> workers);
}
