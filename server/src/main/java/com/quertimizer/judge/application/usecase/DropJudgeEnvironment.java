package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 영속 실행 환경을 제거한다.
 */
@Component
@RequiredArgsConstructor
public class DropJudgeEnvironment {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 영속 실행 환경을 제거한다.
     *
     * @param environmentId 제거할 실행 환경 ID
     */
    public void execute(JudgeEnvironmentId environmentId) {
        judgeRuntime.drop(environmentId);
    }
}
