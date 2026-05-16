package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.monitoring.application.output.DockerContainerOutput;
import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;

import java.util.List;

public interface DockerContainerPort {

    List<DockerContainerOutput> findJudgeContainers(List<DatabaseNodeOutput> nodes);
}
