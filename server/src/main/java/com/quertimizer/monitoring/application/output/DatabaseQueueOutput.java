package com.quertimizer.monitoring.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class DatabaseQueueOutput {

    private final DbmsType dbmsType;
    private final int waitingCount;
}
