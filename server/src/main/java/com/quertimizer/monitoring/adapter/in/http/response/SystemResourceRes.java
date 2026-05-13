package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.monitoring.application.output.SystemResourceOutput;
import lombok.Data;

@Data
public class SystemResourceRes {

    private final double systemCpuUsagePercent;
    private final double processCpuUsagePercent;
    private final double systemLoadAverage;
    private final long totalMemoryBytes;
    private final long usedMemoryBytes;
    private final long totalDiskBytes;
    private final long usedDiskBytes;
    private final long uptimeSeconds;

    public static SystemResourceRes from(SystemResourceOutput output) {
        return new SystemResourceRes(
                output.getSystemCpuUsagePercent(), output.getProcessCpuUsagePercent(), output.getSystemLoadAverage(),
                output.getTotalMemoryBytes(), output.getUsedMemoryBytes(),
                output.getTotalDiskBytes(), output.getUsedDiskBytes(), output.getUptimeSeconds()
        );
    }
}
