package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.monitoring.application.output.MonitoringDatabaseSnapshotOutput;

public interface MonitoringDatabasePort {

    MonitoringDatabaseSnapshotOutput getSnapshot();
}
