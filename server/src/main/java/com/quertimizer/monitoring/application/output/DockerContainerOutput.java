package com.quertimizer.monitoring.application.output;

import lombok.Data;

@Data
public class DockerContainerOutput {

    private final String name;
    private final String status;
    private final String cpuPercent;
    private final String memoryUsage;
}
