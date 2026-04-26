package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelInteractiveExecution {

    private final JudgeQueryService judgeQueryService;

    public void execute(String socketId) {
        // 인터랙티브 SQL 실행 취소를 judge 도메인 use case로 처리
        judgeQueryService.cancelInteractiveExecution(socketId);
    }
}
