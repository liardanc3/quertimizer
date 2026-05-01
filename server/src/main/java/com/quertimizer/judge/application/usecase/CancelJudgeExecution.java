package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 judge 실행을 취소한다.
 */
@Component
@RequiredArgsConstructor
public class CancelJudgeExecution {

    private final JudgeRuntime judgeRuntime;

    /**
     * 진행 중인 judge 실행을 취소한다.
     *
     * @param executionId 취소할 실행 ID
     */
    public void execute(JudgeExecutionId executionId) {
        judgeRuntime.cancel(executionId);
    }
}
