package com.quertimizer.monitoring.adapter.out.docker;

import com.quertimizer.monitoring.application.output.DockerContainerOutput;
import com.quertimizer.monitoring.application.port.out.DockerContainerPort;
import com.quertimizer.monitoring.application.port.out.MonitoringDatabasePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DockerContainerAdapter implements DockerContainerPort {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    private final MonitoringDatabasePort monitoringDatabasePort;

    @Override
    public List<DockerContainerOutput> findJudgeContainers() {
        // DB 노드 설정의 container 이름 기준 docker stats 조회
        Set<String> containerNames = new LinkedHashSet<>();
        for (var node : monitoringDatabasePort.getNodes()) {
            if (node.getContainerName() != null && !node.getContainerName().isBlank()) {
                containerNames.add(node.getContainerName().trim());
            }
        }
        if (containerNames.isEmpty()) {
            return List.of();
        }

        return containerNames.stream()
                .map(this::readContainer)
                .toList();
    }

    private DockerContainerOutput readContainer(String containerName) {
        // docker inspect와 stats 결과를 컨테이너 출력 모델로 변환
        String status = runCommand(List.of("docker", "inspect", "--format", "{{.State.Status}}", containerName));
        String stats = runCommand(List.of("docker", "stats", "--no-stream", "--format", "{{.CPUPerc}}\t{{.MemUsage}}", containerName));
        String[] tokens = stats.split("\\t", 2);
        String cpuPercent = tokens.length > 0 && !tokens[0].isBlank() ? tokens[0].trim() : "-";
        String memoryUsage = tokens.length > 1 && !tokens[1].isBlank() ? tokens[1].trim() : "-";
        return new DockerContainerOutput(containerName, status.isBlank() ? "unknown" : status.trim(), cpuPercent, memoryUsage);
    }

    private String runCommand(List<String> command) {
        // docker 명령 실패 시 UI가 깨지지 않도록 빈 값 반환
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0) {
                return "";
            }

            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
