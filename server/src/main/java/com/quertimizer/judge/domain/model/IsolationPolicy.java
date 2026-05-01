package com.quertimizer.judge.domain.model;

public class IsolationPolicy {

    private final boolean createTemporaryEnvironment;
    private final boolean initializeStatisticsAfterLoad;
    private final boolean applySetupSqls;
    private final boolean initializeStatisticsAfterSetup;
    private final boolean dropEnvironmentAfterExecution;

    public IsolationPolicy(boolean createTemporaryEnvironment,
                           boolean initializeStatisticsAfterLoad,
                           boolean applySetupSqls,
                           boolean initializeStatisticsAfterSetup,
                           boolean dropEnvironmentAfterExecution) {
        this.createTemporaryEnvironment = createTemporaryEnvironment;
        this.initializeStatisticsAfterLoad = initializeStatisticsAfterLoad;
        this.applySetupSqls = applySetupSqls;
        this.initializeStatisticsAfterSetup = initializeStatisticsAfterSetup;
        this.dropEnvironmentAfterExecution = dropEnvironmentAfterExecution;
    }

    public static IsolationPolicy cleanRoom() {
        return new IsolationPolicy(true, true, true, true, true);
    }

    public boolean isCreateTemporaryEnvironment() {
        return createTemporaryEnvironment;
    }

    public boolean isInitializeStatisticsAfterLoad() {
        return initializeStatisticsAfterLoad;
    }

    public boolean isApplySetupSqls() {
        return applySetupSqls;
    }

    public boolean isInitializeStatisticsAfterSetup() {
        return initializeStatisticsAfterSetup;
    }

    public boolean isDropEnvironmentAfterExecution() {
        return dropEnvironmentAfterExecution;
    }
}
