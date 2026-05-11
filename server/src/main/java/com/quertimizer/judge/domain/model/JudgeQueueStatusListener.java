package com.quertimizer.judge.domain.model;

public interface JudgeQueueStatusListener {

    void onWaiting(int remainingTasks);

    default void onSnapshotCreating() {
    }

    default void onSnapshotCreated() {
    }

    default void onProcessStarting(DbmsType dbmsType) {
    }

    default void onProcessStarted(DbmsType dbmsType) {
    }

    static JudgeQueueStatusListener noop() {
        return remainingTasks -> {
        };
    }
}
