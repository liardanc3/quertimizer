package com.quertimizer.judge.domain.model;

import lombok.Data;

@Data
public class IndexPolicy {

    private final boolean keepBaseIndexes;
    private final boolean applySetupIndexesOnly;

    public IndexPolicy(boolean keepBaseIndexes, boolean applySetupIndexesOnly) {
        this.keepBaseIndexes = keepBaseIndexes;
        this.applySetupIndexesOnly = applySetupIndexesOnly;
    }
}
