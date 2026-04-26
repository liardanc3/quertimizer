package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.service.JudgeAnswerHashSupport;

import java.util.List;

public final class ProblemAnswerHashSupport {

    private ProblemAnswerHashSupport() {
    }

    public static String hashRows(List<List<String>> rows) {
        // judge 도메인으로 이동한 정답 해시 계산 로직을 호환 경로로 위임
        return JudgeAnswerHashSupport.hashRows(rows);
    }
}
