package com.quertimizer.sqljudge.policy;

/**
 * Represents statistics initialization policy.
 */
public class StatisticsPolicy {

    private final boolean analyzeAfterLoad;
    private final boolean analyzeAfterIndex;

    /**
     * Creates a statistics initialization policy.
     *
     * @param analyzeAfterLoad whether statistics should be analyzed after data load
     * @param analyzeAfterIndex whether statistics should be analyzed after index changes
     */
    public StatisticsPolicy(boolean analyzeAfterLoad, boolean analyzeAfterIndex) {
        this.analyzeAfterLoad = analyzeAfterLoad;
        this.analyzeAfterIndex = analyzeAfterIndex;
    }

    /**
     * Returns whether statistics should be analyzed after data load.
     *
     * @return true when statistics should be analyzed after data load
     */
    public boolean isAnalyzeAfterLoad() {
        return analyzeAfterLoad;
    }

    /**
     * Returns whether statistics should be analyzed after index changes.
     *
     * @return true when statistics should be analyzed after index changes
     */
    public boolean isAnalyzeAfterIndex() {
        return analyzeAfterIndex;
    }
}
