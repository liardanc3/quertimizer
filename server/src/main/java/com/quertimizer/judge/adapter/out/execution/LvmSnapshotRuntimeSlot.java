package com.quertimizer.judge.adapter.out.execution;

import java.util.Objects;

class LvmSnapshotRuntimeSlot {

    private final RuntimeDatabaseLease runnerLease;
    private final int port;

    LvmSnapshotRuntimeSlot(RuntimeDatabaseLease runnerLease, int port) {
        this.runnerLease = Objects.requireNonNull(runnerLease, "필수 값이 없습니다.");
        this.port = port;
    }

    RuntimeDatabaseLease getRunnerLease() {
        return runnerLease;
    }

    RuntimeDatabase getRunnerDatabase() {
        return runnerLease.getDatabase();
    }

    int getPort() {
        return port;
    }
}
