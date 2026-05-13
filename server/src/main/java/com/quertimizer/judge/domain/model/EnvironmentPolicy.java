package com.quertimizer.judge.domain.model;

import lombok.Data;

@Data
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
        return new EnvironmentPolicy(true, true, false);
    }
}
