package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;
import com.quertimizer.user.application.service.UserAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserAnomalyTrends {

    private final UserAnomalyDetectionService userAnomalyDetectionService;

    public UserAnomalyTrendPageOutput execute(String range, String startedAt, String endedAt, int page, Integer pageSize) {
        // 이상 제출 추세를 조회
        return userAnomalyDetectionService.getSubmitTrend(range, startedAt, endedAt, page, pageSize);
    }
}
