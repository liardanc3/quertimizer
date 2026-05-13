package com.quertimizer.dashboard.adapter.out.problem;

import com.quertimizer.dashboard.application.port.out.DashboardProblemPort;
import com.quertimizer.dashboard.domain.model.DashboardProblemCandidate;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemRecommendationCandidatesInput;
import com.quertimizer.problem.application.output.ProblemRecommendationCandidateOutput;
import com.quertimizer.problem.application.port.in.FindProblemRecommendationCandidatesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dashboardProblemGateway")
@RequiredArgsConstructor
public class ProblemGateway implements DashboardProblemPort {

    private final FindProblemRecommendationCandidatesUseCase findProblemRecommendationCandidates;

    @Override
    public List<DashboardProblemCandidate> findProblemCandidates(DbmsType dbmsType, String currentHandle, int candidateLimit) {
        // problem use case 기준 추천 문제 후보 조회
        return findProblemRecommendationCandidates.execute(
                        new ProblemRecommendationCandidatesInput(dbmsType, currentHandle, candidateLimit)
                )
                .stream()
                .map(this::toCandidate)
                .toList();
    }

    private DashboardProblemCandidate toCandidate(ProblemRecommendationCandidateOutput output) {
        // problem 추천 후보 응답을 dashboard 후보 모델로 변환
        return new DashboardProblemCandidate(
                output.getProblemId(), output.getTitle(), output.getDbms(),
                output.getSolvedUserCount(), output.getTotalSubmitCount(),
                output.getSuccessSubmitCount(), output.isSolvedByCurrentUser()
        );
    }

}
