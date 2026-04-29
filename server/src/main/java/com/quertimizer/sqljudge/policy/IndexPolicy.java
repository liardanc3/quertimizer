package com.quertimizer.sqljudge.policy;

/**
 * Represents index handling policy.
 */
public class IndexPolicy {

    private final boolean keepBaseIndexes;
    private final boolean applySetupIndexesOnly;

    /**
     * Creates an index handling policy.
     *
     * @param keepBaseIndexes whether base indexes should be kept
     * @param applySetupIndexesOnly whether only setup indexes should be applied
     */
    public IndexPolicy(boolean keepBaseIndexes, boolean applySetupIndexesOnly) {
        this.keepBaseIndexes = keepBaseIndexes;
        this.applySetupIndexesOnly = applySetupIndexesOnly;
    }

    /**
     * Returns whether base indexes should be kept.
     *
     * @return true when base indexes should be kept
     */
    public boolean isKeepBaseIndexes() {
        return keepBaseIndexes;
    }

    /**
     * Returns whether only setup indexes should be applied.
     *
     * @return true when only setup indexes should be applied
     */
    public boolean isApplySetupIndexesOnly() {
        return applySetupIndexesOnly;
    }
}
