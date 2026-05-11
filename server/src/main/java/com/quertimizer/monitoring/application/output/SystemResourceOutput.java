package com.quertimizer.monitoring.application.output;

import lombok.Data;

@Data
public class SystemResourceOutput {

    private final double systemCpuUsagePercent;
    private final double processCpuUsagePercent;
    private final double systemLoadAverage;
    private final long totalMemoryBytes;
    private final long usedMemoryBytes;
    private final long totalDiskBytes;
    private final long usedDiskBytes;
    private final long uptimeSeconds;
}
