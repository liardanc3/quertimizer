package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.input.ProblemUserSubmitCountInput;
import com.quertimizer.problem.application.output.ProblemUserSubmitCountOutput;
import com.quertimizer.problem.application.port.in.GetProblemUserSubmitCountsUseCase;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetProblemUserSubmitCounts implements GetProblemUserSubmitCountsUseCase {

    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;

    /**
     * 사용자별 문제 제출 횟수를 조회한다.
     *
     * <ol>
     *   <li>검색 시간 범위에 맞는 제출 집계 조회
     *   <li>공개 응답 모델로 변환
     * </ol>
     *
     * @param input 제출 집계 검색 조건
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProblemUserSubmitCountOutput> execute(ProblemUserSubmitCountInput input) {
        Page<ProblemSubmitHistoryRepositoryPort.UserSubmitCountProjection> submitCountPage = resolveSubmitCountPage(input);

        return submitCountPage.map(projection -> new ProblemUserSubmitCountOutput(
                projection.getHandle(), projection.getSubmitCount()
        ));
    }

    private Page<ProblemSubmitHistoryRepositoryPort.UserSubmitCountProjection> resolveSubmitCountPage(ProblemUserSubmitCountInput input) {
        // 제출 집계 검색 범위 선택
        if (input.getSubmittedStart() != null && input.getSubmittedEnd() != null) {
            return problemSubmitHistoryRepository.findUserSubmitCountsBetween(
                    input.getSubmittedStart(), input.getSubmittedEnd(), input.getPageable()
            );
        }

        if (input.getSubmittedStart() != null) {
            return problemSubmitHistoryRepository.findUserSubmitCountsSince(input.getSubmittedStart(), input.getPageable());
        }

        return problemSubmitHistoryRepository.findUserSubmitCounts(input.getPageable());
    }
}
