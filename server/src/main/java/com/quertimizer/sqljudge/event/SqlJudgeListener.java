package com.quertimizer.sqljudge.event;

/**
 * Receives sql-judge execution events.
 */
public interface SqlJudgeListener {

    /**
     * Handles a sql-judge execution event.
     *
     * @param event sql-judge execution event
     */
    void onEvent(SqlJudgeEvent event);
}
