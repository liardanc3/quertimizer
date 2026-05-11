package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.monitoring.application.output.JudgeRuntimeNodeOutput;
import com.quertimizer.monitoring.application.output.JudgeRuntimeQueueOutput;

import java.util.List;

public interface MonitoringJudgeRuntimePort {

    int getTotalWaitingCount();

    int getTotalRunningCount();

    List<JudgeRuntimeQueueOutput> getQueues();

    List<JudgeRuntimeNodeOutput> getNodes();
}
