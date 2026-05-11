package com.quertimizer.monitoring.adapter.in.web.response;

import com.quertimizer.monitoring.application.output.DockerContainerOutput;
import lombok.Data;

@Data
public class DockerContainerRes {

    private final String name;
    private final String status;
    private final String cpuPercent;
    private final String memoryUsage;

    public static DockerContainerRes from(DockerContainerOutput output) {
        return new DockerContainerRes(output.getName(), output.getStatus(), output.getCpuPercent(), output.getMemoryUsage());
    }
}
