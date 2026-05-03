package com.quertimizer.judge.adapter.out.execution;

import java.util.List;

public interface RuntimeDatabaseSelector {

    int selectStartIndex(List<RuntimeDatabase> candidates);
}
