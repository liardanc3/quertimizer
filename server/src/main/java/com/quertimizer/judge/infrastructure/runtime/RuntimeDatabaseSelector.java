package com.quertimizer.judge.infrastructure.runtime;

import java.util.List;

public interface RuntimeDatabaseSelector {

    int selectStartIndex(List<RuntimeDatabase> candidates);
}
