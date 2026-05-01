package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 영속 실행 환경을 생성한다.
 */
@Component
@RequiredArgsConstructor
public class CreateJudgeEnvironment {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 영속 실행 환경을 생성한다.
     *
     * @param input 실행 환경 생성 입력
     * @return 생성된 실행 환경 ID
     */
    public JudgeEnvironmentId execute(CreateJudgeEnvironmentInput input) {
        return judgeRuntime.create(input);
    }
}
