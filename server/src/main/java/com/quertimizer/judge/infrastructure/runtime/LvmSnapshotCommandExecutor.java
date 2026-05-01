package com.quertimizer.judge.infrastructure.runtime;

import java.util.List;

public interface LvmSnapshotCommandExecutor {

    String execute(List<String> command);
}
