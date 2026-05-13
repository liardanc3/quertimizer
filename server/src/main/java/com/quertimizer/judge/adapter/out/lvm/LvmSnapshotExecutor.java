package com.quertimizer.judge.adapter.out.lvm;

import com.quertimizer.judge.application.port.out.LvmSnapshotConfigRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_COMMAND_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_COMMAND_FAILED_WITH_OUTPUT;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_COMMAND_INTERRUPTED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_COMMAND_TIMEOUT;

@Component
@RequiredArgsConstructor
public class LvmSnapshotExecutor {

    private final LvmSnapshotConfigRepositoryPort lvmSnapshotConfigRepositoryPort;

    public void executeAll(List<List<String>> commands) {
        // LVM 파일시스템 명령 목록 순차 실행
        for (List<String> command : commands) {
            execute(command);
        }
    }

    public String execute(List<String> command) {
        // LVM 파일시스템 명령 실행과 결과 검증
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(commandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(LVM_COMMAND_TIMEOUT.format(command));
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException(LVM_COMMAND_FAILED_WITH_OUTPUT.format(command, System.lineSeparator(), output));
            }

            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(LVM_COMMAND_INTERRUPTED.format(command), exception);
        } catch (Exception exception) {
            throw new IllegalStateException(LVM_COMMAND_FAILED.format(command), exception);
        }
    }

    private int commandTimeoutSeconds() {
        // DB DB 실행 환경 설정의 LVM 명령 제한 시간 조회
        return lvmSnapshotConfigRepositoryPort.findDefault()
                .map(config -> config.getCommandTimeoutSeconds() > 0 ? config.getCommandTimeoutSeconds() : 60)
                .orElse(60);
    }
}
