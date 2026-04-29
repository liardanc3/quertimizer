package com.quertimizer.sqljudge.policy;

/**
 * Represents environment creation policy.
 */
public class EnvironmentPolicy {

    private final boolean initializeStatisticsAfterLoad;
    private final boolean applyBaseIndexes;
    private final boolean reusable;

    /**
     * Creates an environment creation policy.
     *
     * @param initializeStatisticsAfterLoad whether statistics should be initialized after data load
     * @param applyBaseIndexes whether base indexes should be applied
     * @param reusable whether the environment can be reused
     */
    public EnvironmentPolicy(boolean initializeStatisticsAfterLoad, boolean applyBaseIndexes, boolean reusable) {
        this.initializeStatisticsAfterLoad = initializeStatisticsAfterLoad;
        this.applyBaseIndexes = applyBaseIndexes;
        this.reusable = reusable;
    }

    /**
     * Creates a policy for interactive environments.
     *
     * @return interactive environment policy
     */
    public static EnvironmentPolicy interactive() {
        return new EnvironmentPolicy(true, true, true);
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
     * Returns whether base indexes should be applied.
     *
     * @return true when base indexes should be applied
     */
    public boolean isApplyBaseIndexes() {
        return applyBaseIndexes;
    }

    /**
     * Returns whether the environment can be reused.
     *
     * @return true when the environment can be reused
     */
    public boolean isReusable() {
        return reusable;
    }
}
