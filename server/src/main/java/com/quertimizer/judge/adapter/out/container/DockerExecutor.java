package com.quertimizer.judge.adapter.out.container;

import com.quertimizer.judge.application.port.out.ContainerPort;
import com.quertimizer.judge.application.port.out.LvmSnapshotConfigRepositoryPort;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.application.model.Names;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.quertimizer.judge.domain.model.JudgeFailReason.DOCKER_COMMAND_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.DOCKER_COMMAND_FAILED_WITH_OUTPUT;
import static com.quertimizer.judge.domain.model.JudgeFailReason.DOCKER_COMMAND_INTERRUPTED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.DOCKER_COMMAND_TIMEOUT;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerExecutor implements ContainerPort {

    private final DockerCommandFactory dockerCommandFactory;
    private final LvmSnapshotConfigRepositoryPort lvmSnapshotConfigRepositoryPort;

    @Override
    public void startEvalProcess(DbmsType dbmsType, String containerName, String environmentId, int port) {
        // 평가 snapshot을 바라보는 컨테이너 내부 DB 프로세스 시작
        execute(dockerCommandFactory.startEvalProcessCommand(dbmsType, containerName, environmentId, port));
    }

    @Override
    public void stopEvalProcess(DbmsType dbmsType, String containerName,
                                String environmentId, String mysqlRootPassword) {
        // 평가 snapshot을 바라보는 컨테이너 내부 DB 프로세스 정지
        execute(dockerCommandFactory.stopEvalProcessCommand(dbmsType, containerName, environmentId, mysqlRootPassword));
    }

    @Override
    public void startTemplateProcess(DbmsType dbmsType, String containerName, String templateVersion, int port) {
        // 템플릿 snapshot을 바라보는 컨테이너 내부 DB 프로세스 시작
        execute(dockerCommandFactory.startTemplateProcessCommand(dbmsType, containerName, templateVersion, port));
    }

    @Override
    public void stopTemplateProcess(DbmsType dbmsType, String containerName,
                                    String templateVersion, String mysqlRootPassword) {
        // 템플릿 snapshot을 바라보는 컨테이너 내부 DB 프로세스 정지
        execute(dockerCommandFactory.stopTemplateProcessCommand(dbmsType, containerName, templateVersion, mysqlRootPassword));
    }

    @Override
    public void stopOrphanEvalProcesses(DbmsType dbmsType, List<DatabaseNode> databaseNodes,
                                        String environmentId) {
        // 전체 컨테이너에서 고아 평가 DB 프로세스 정리
        String normalizedEnvironmentId = Names.scriptName(environmentId);
        for (DatabaseNode databaseNode : databaseNodes) {
            try {
                stopEvalProcess(
                        dbmsType, databaseNode.getContainerName(),
                        normalizedEnvironmentId, databaseNode.getRootPassword()
                );
            } catch (Exception exception) {
                log.info(
                        "[고아 리소스 정리] DB 프로세스 정리 생략 container={} environment={}",
                        databaseNode.getContainerName(), normalizedEnvironmentId
                );
            }
        }
    }

    private String execute(List<String> command) {
        // Docker 명령 실행과 결과 검증
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(commandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(DOCKER_COMMAND_TIMEOUT.format(command));
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException(DOCKER_COMMAND_FAILED_WITH_OUTPUT.format(command, System.lineSeparator(), output));
            }

            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(DOCKER_COMMAND_INTERRUPTED.format(command), exception);
        } catch (Exception exception) {
            throw new IllegalStateException(DOCKER_COMMAND_FAILED.format(command), exception);
        }
    }

    private int commandTimeoutSeconds() {
        // DB DB 실행 환경 설정의 Docker 명령 제한 시간 조회
        return lvmSnapshotConfigRepositoryPort.findDefault()
                .map(config -> config.getCommandTimeoutSeconds() > 0 ? config.getCommandTimeoutSeconds() : 60)
                .orElse(60);
    }
}
