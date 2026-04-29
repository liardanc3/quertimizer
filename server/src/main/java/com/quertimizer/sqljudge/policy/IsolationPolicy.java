package com.quertimizer.sqljudge.policy;

/**
 * Represents isolated execution policy.
 */
public class IsolationPolicy {

    private final boolean createTemporaryEnvironment;
    private final boolean initializeStatisticsAfterLoad;
    private final boolean applySetupSqls;
    private final boolean initializeStatisticsAfterSetup;
    private final boolean dropEnvironmentAfterExecution;

    /**
     * Creates an isolated execution policy.
     *
     * @param createTemporaryEnvironment whether a temporary environment should be created
     * @param initializeStatisticsAfterLoad whether statistics should be initialized after data load
     * @param applySetupSqls whether setup SQL statements should be applied
     * @param initializeStatisticsAfterSetup whether statistics should be initialized after setup SQL statements
     * @param dropEnvironmentAfterExecution whether the environment should be dropped after execution
     */
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

    /**
     * Creates a clean-room isolated execution policy.
     *
     * @return clean-room isolated execution policy
     */
    public static IsolationPolicy cleanRoom() {
        return new IsolationPolicy(true, true, true, true, true);
    }

    /**
     * Returns whether a temporary environment should be created.
     *
     * @return true when a temporary environment should be created
     */
    public boolean isCreateTemporaryEnvironment() {
        return createTemporaryEnvironment;
    }

    /**
     * Returns whether statistics should be initialized after data load.
     *
     * @return true when statistics should be initialized after data load
     */
    public boolean isInitializeStatisticsAfterLoad() {
        return initializeStatisticsAfterLoad;
    }

    /**
     * Returns whether setup SQL statements should be applied.
     *
     * @return true when setup SQL statements should be applied
     */
    public boolean isApplySetupSqls() {
        return applySetupSqls;
    }

    /**
     * Returns whether statistics should be initialized after setup SQL statements.
     *
     * @return true when statistics should be initialized after setup SQL statements
     */
    public boolean isInitializeStatisticsAfterSetup() {
        return initializeStatisticsAfterSetup;
    }

    /**
     * Returns whether the environment should be dropped after execution.
     *
     * @return true when the environment should be dropped after execution
     */
    public boolean isDropEnvironmentAfterExecution() {
        return dropEnvironmentAfterExecution;
    }
}
