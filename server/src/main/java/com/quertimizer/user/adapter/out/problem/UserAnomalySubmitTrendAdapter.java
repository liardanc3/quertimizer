package com.quertimizer.user.adapter.out.problem;

import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.user.application.port.out.UserAnomalySubmitTrendPort;
import com.quertimizer.user.domain.model.UserAnomalySubmitTrend;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserAnomalySubmitTrendAdapter implements UserAnomalySubmitTrendPort {

    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCounts(Pageable pageable) {
        // problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCounts(pageable).map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable) {
        // 기준 시간 이후 problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCountsSince(submittedAfter, pageable).map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                    LocalDateTime submittedEnd,
                                                                    Pageable pageable) {
        // 기준 시간 범위 problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable)
                .map(this::toSubmitTrend);
    }

    private UserAnomalySubmitTrend toSubmitTrend(ProblemSubmitHistoryRepositoryPort.UserSubmitCountProjection projection) {
        // problem 제출 집계 projection을 user 이상 제출 추세 모델로 변환
        return new UserAnomalySubmitTrend(projection.getHandle(), projection.getSubmitCount());
    }

}
