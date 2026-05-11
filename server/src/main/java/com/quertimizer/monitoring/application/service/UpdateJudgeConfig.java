package com.quertimizer.monitoring.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.monitoring.application.input.JudgeConfigUpdateInput;
import com.quertimizer.monitoring.application.output.JudgeConfigOutput;
import com.quertimizer.monitoring.application.output.JudgeRuntimeNodeOutput;
import com.quertimizer.monitoring.application.port.in.UpdateJudgeConfigUseCase;
import com.quertimizer.monitoring.application.port.out.JudgeConfigRepositoryPort;
import com.quertimizer.monitoring.application.port.out.MonitoringJudgeRuntimePort;
import com.quertimizer.monitoring.domain.entity.JudgeConfig;
import com.quertimizer.monitoring.domain.model.MonitoringFailReason;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateJudgeConfig implements UpdateJudgeConfigUseCase {

    private final JudgeConfigRepositoryPort judgeConfigRepositoryPort;
    private final MonitoringJudgeRuntimePort monitoringJudgeRuntimePort;

    /**
     * judge runtime 동적 설정을 저장한다.
     *
     * <ol>
     *   <li>설정 대상 runtime 노드 조회
     *   <li>동시 실행 수 검증
     *   <li>기존 설정 변경 또는 신규 설정 저장
     * </ol>
     *
     * @param input 변경할 database와 동시 실행 설정
     */
    @Override
    @Transactional
    public JudgeConfigOutput execute(JudgeConfigUpdateInput input) {
        JudgeRuntimeNodeOutput node = monitoringJudgeRuntimePort.getNodes().stream()
                .filter(candidate -> candidate.getDatabaseId().equals(input.getDatabaseId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(MonitoringFailReason.JUDGE_CONFIG_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        if (input.getMaxConcurrency() <= 0 || input.getMaxConcurrency() > node.getTotalPortCount()) {
            throw new BusinessException(MonitoringFailReason.INVALID_MAX_CONCURRENCY.getMessage(), HttpStatus.BAD_REQUEST);
        }

        JudgeConfig judgeConfig = judgeConfigRepositoryPort.findByDatabaseId(input.getDatabaseId())
                .map(config -> config.update(input.isEnabled(), input.getMaxConcurrency()))
                .orElseGet(() -> JudgeConfig.create(
                        node.getDatabaseId(), node.getDatabaseName(), node.getDbmsType(),
                        input.isEnabled(), input.getMaxConcurrency()
                ));
        return JudgeConfigOutput.from(judgeConfigRepositoryPort.save(judgeConfig));
    }
}
