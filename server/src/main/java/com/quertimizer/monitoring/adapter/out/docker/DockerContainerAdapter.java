package com.quertimizer.monitoring.adapter.out.docker;

import com.quertimizer.monitoring.application.output.DockerContainerOutput;
import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;
import com.quertimizer.monitoring.application.port.out.DockerContainerPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class DockerContainerAdapter implements DockerContainerPort {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    @Override
    public List<DockerContainerOutput> findJudgeContainers(List<DatabaseNodeOutput> nodes) {
        // DB 노드 설정의 container 이름 기준 docker stats 조회
        Set<String> containerNames = new LinkedHashSet<>();
        for (var node : nodes) {
            if (node.getContainerName() != null && !node.getContainerName().isBlank()) {
                containerNames.add(node.getContainerName().trim());
            }
        }
        if (containerNames.isEmpty()) {
            return List.of();
        }

        Map<String, String> statusByContainerName = readContainerStatuses(containerNames);
        Map<String, DockerStats> statsByContainerName = readContainerStats(containerNames);
        return containerNames.stream()
                .map(containerName -> new DockerContainerOutput(
                        containerName,
                        statusByContainerName.getOrDefault(containerName, "unknown"),
                        statsByContainerName.getOrDefault(containerName, DockerStats.empty()).cpuPercent,
                        statsByContainerName.getOrDefault(containerName, DockerStats.empty()).memoryUsage
                ))
                .toList();
    }

    private Map<String, String> readContainerStatuses(Set<String> containerNames) {
        // docker inspect batch 결과를 컨테이너 이름별 상태로 변환
        List<String> command = new ArrayList<>(List.of("docker", "inspect", "--format", "{{.Name}}\t{{.State.Status}}"));
        command.addAll(containerNames);
        Map<String, String> statusByContainerName = new LinkedHashMap<>();
        for (String line : runCommand(command).split("\\R")) {
            String[] tokens = line.split("\\t", 2);
            if (tokens.length == 2 && !tokens[0].isBlank()) {
                statusByContainerName.put(tokens[0].replaceFirst("^/", "").trim(), tokens[1].isBlank() ? "unknown" : tokens[1].trim());
            }
        }

        return statusByContainerName;
    }

    private Map<String, DockerStats> readContainerStats(Set<String> containerNames) {
        // docker stats batch 결과를 컨테이너 이름별 리소스 사용량으로 변환
        List<String> command = new ArrayList<>(List.of("docker", "stats", "--no-stream", "--format", "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"));
        command.addAll(containerNames);
        Map<String, DockerStats> statsByContainerName = new LinkedHashMap<>();
        for (String line : runCommand(command).split("\\R")) {
            String[] tokens = line.split("\\t", 3);
            if (tokens.length >= 3 && !tokens[0].isBlank()) {
                statsByContainerName.put(tokens[0].trim(), new DockerStats(
                        tokens[1].isBlank() ? "-" : tokens[1].trim(),
                        tokens[2].isBlank() ? "-" : tokens[2].trim()
                ));
            }
        }

        return statsByContainerName;
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

    private static final class DockerStats {
        private final String cpuPercent;
        private final String memoryUsage;

        private DockerStats(String cpuPercent, String memoryUsage) {
            this.cpuPercent = cpuPercent;
            this.memoryUsage = memoryUsage;
        }

        private static DockerStats empty() {
            return new DockerStats("-", "-");
        }
    }
}
