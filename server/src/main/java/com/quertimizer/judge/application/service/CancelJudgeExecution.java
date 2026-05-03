package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.CancelJudgeExecutionUseCase;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelJudgeExecution implements CancelJudgeExecutionUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * 진행 중인 judge 실행을 취소한다.
     *
     * @param executionId 취소할 실행 ID
     */
    @Override
    public void execute(JudgeExecutionId executionId) {
        judgeRuntime.cancel(executionId);
    }
}
