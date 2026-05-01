package com.quertimizer.judge.domain.model;

public class IndexPolicy {

    private final boolean keepBaseIndexes;
    private final boolean applySetupIndexesOnly;

    public IndexPolicy(boolean keepBaseIndexes, boolean applySetupIndexesOnly) {
        this.keepBaseIndexes = keepBaseIndexes;
        this.applySetupIndexesOnly = applySetupIndexesOnly;
    }

    public boolean isKeepBaseIndexes() {
        return keepBaseIndexes;
    }

    public boolean isApplySetupIndexesOnly() {
        return applySetupIndexesOnly;
    }
}
