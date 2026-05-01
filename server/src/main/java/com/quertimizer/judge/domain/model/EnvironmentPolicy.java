package com.quertimizer.judge.domain.model;

public class EnvironmentPolicy {

    private final boolean initializeStatisticsAfterLoad;
    private final boolean applyBaseIndexes;
    private final boolean reusable;

    public EnvironmentPolicy(boolean initializeStatisticsAfterLoad, boolean applyBaseIndexes, boolean reusable) {
        this.initializeStatisticsAfterLoad = initializeStatisticsAfterLoad;
        this.applyBaseIndexes = applyBaseIndexes;
        this.reusable = reusable;
    }

    public static EnvironmentPolicy interactive() {
        return new EnvironmentPolicy(true, true, true);
    }

    public boolean isInitializeStatisticsAfterLoad() {
        return initializeStatisticsAfterLoad;
    }

    public boolean isApplyBaseIndexes() {
        return applyBaseIndexes;
    }

    public boolean isReusable() {
        return reusable;
    }
}
