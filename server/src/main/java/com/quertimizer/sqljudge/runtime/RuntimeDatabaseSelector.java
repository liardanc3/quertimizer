package com.quertimizer.sqljudge.runtime;

import java.util.List;

/**
 * Selects the start index for runtime database node selection.
 */
public interface RuntimeDatabaseSelector {

    /**
     * Selects a start index from candidate runtime databases.
     *
     * @param candidates runtime database candidates
     * @return selected start index
     */
    int selectStartIndex(List<RuntimeDatabase> candidates);
}
