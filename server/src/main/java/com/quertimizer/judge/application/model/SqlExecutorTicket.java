package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import lombok.Data;

import java.util.UUID;

@Data
public class SqlExecutorTicket {

    private final String ticketId;
    private final DbmsType dbmsType;
    private final QueuePriority priority;
    private final QueueStatusListener statusListener;

    public SqlExecutorTicket(DbmsType dbmsType, QueuePriority priority, QueueStatusListener statusListener) {
        this.ticketId = "ticket-" + UUID.randomUUID();
        this.dbmsType = dbmsType;
        this.priority = priority != null ? priority : QueuePriority.NORMAL;
        this.statusListener = statusListener != null ? statusListener : QueueStatusListener.noop();
    }
}
