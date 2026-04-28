package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelInteractiveExecution {

    private final JudgeQueryService judgeQueryService;

    /**
     * 진행 중인 인터랙티브 SQL 실행을 취소한다.
     *
     * @param socketId 취소할 실행의 소켓 ID
     */
    public void execute(String socketId) {
        judgeQueryService.cancelInteractiveExecution(socketId);
    }
}
