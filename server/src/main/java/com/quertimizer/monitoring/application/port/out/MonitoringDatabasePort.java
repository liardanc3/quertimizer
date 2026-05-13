package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;
import com.quertimizer.monitoring.application.output.DatabaseQueueOutput;

import java.util.List;

public interface MonitoringDatabasePort {

    int getTotalWaitingCount();

    int getTotalRunningCount();

    List<DatabaseQueueOutput> getQueues();

    List<DatabaseNodeOutput> getNodes();
}
