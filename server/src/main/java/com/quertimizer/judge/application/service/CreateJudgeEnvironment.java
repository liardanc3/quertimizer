package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.CreateJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateJudgeEnvironment implements CreateJudgeEnvironmentUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 영속 실행 환경을 생성한다.
     *
     * @param input 실행 환경 생성 입력
     * @return 생성된 실행 환경 ID
     */
    @Override
    public JudgeEnvironmentId execute(CreateJudgeEnvironmentInput input) {
        return judgeRuntime.create(input);
    }
}
