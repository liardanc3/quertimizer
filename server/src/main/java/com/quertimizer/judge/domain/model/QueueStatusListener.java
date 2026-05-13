package com.quertimizer.judge.domain.model;

public interface QueueStatusListener {

    void onWaiting(int remainingTasks);

    default void onSnapshotCreating() {
    }

    default void onSnapshotCreated() {
    }

    default void onProcessStarting(DbmsType dbmsType) {
    }

    default void onProcessStarted(DbmsType dbmsType) {
    }

    static QueueStatusListener noop() {
        return remainingTasks -> {
        };
    }
}
