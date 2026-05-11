package com.quertimizer.monitoring.adapter.in.web.response;

import com.quertimizer.monitoring.application.output.JudgeRuntimeQueueOutput;
import lombok.Data;

@Data
public class JudgeRuntimeQueueRes {

    private final String dbmsType;
    private final String dbmsLabel;
    private final int waitingCount;

    public static JudgeRuntimeQueueRes from(JudgeRuntimeQueueOutput output) {
        return new JudgeRuntimeQueueRes(output.getDbmsType().getValue(), output.getDbmsType().getLabel(), output.getWaitingCount());
    }
}
