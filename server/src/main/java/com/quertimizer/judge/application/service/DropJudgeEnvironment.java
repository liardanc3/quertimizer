package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.DropJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DropJudgeEnvironment implements DropJudgeEnvironmentUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 영속 실행 환경을 제거한다.
     *
     * @param environmentId 제거할 실행 환경 ID
     */
    @Override
    public void execute(JudgeEnvironmentId environmentId) {
        judgeRuntime.drop(environmentId);
    }
}
