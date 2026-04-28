package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserAnomalyTrendSearchInput;
import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;
import com.quertimizer.user.application.service.UserAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserAnomalyTrends {

    private final UserAnomalyDetectionService userAnomalyDetectionService;

    /**
     * 이상 제출 추세를 조회한다.
     *
     * @param input 이상 제출 추세 검색 입력
     */
    public UserAnomalyTrendPageOutput execute(UserAnomalyTrendSearchInput input) {
        return userAnomalyDetectionService.getSubmitTrend(
                input.getRange(), input.getStartedAt(), input.getEndedAt(), input.getPage(), input.getPageSize()
        );
    }
}
