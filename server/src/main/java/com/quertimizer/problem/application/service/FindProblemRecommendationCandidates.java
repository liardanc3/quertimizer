package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.input.ProblemRecommendationCandidatesInput;
import com.quertimizer.problem.application.output.ProblemListEntry;
import com.quertimizer.problem.application.output.ProblemRecommendationCandidateOutput;
import com.quertimizer.problem.application.port.in.FindProblemRecommendationCandidatesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FindProblemRecommendationCandidates implements FindProblemRecommendationCandidatesUseCase {

    private final ProblemSearchService problemSearchService;

    /**
     * 대시보드 추천 문제 후보를 조회한다.
     *
     * <ol>
     *   <li>풀이 상태와 정렬 기준별 후보 조회
     *   <li>중복 후보 제거
     *   <li>대시보드에 노출 가능한 문제 후보 응답 변환
     * </ol>
     *
     * @param input 추천 후보 조회 조건
     */
    @Transactional(readOnly = true)
    @Override
    public List<ProblemRecommendationCandidateOutput> execute(ProblemRecommendationCandidatesInput input) {
        Map<String, ProblemListEntry> candidatesByProblemId = new LinkedHashMap<>();
        String solveState = input.getCurrentHandle() == null ? "all" : "unsolved";

        addProblemCandidates(candidatesByProblemId, findCandidates(input, solveState, "desc", "none", "none"));
        addProblemCandidates(candidatesByProblemId, findCandidates(input, solveState, "none", "desc", "none"));
        addProblemCandidates(candidatesByProblemId, findCandidates(input, solveState, "none", "none", "desc"));

        return candidatesByProblemId.values().stream()
                .map(this::toOutput)
                .toList();
    }

    private List<ProblemListEntry> findCandidates(ProblemRecommendationCandidatesInput input, String solveState,
                                                  String solvedCountSort, String totalSubmitSort,
                                                  String successSubmitSort) {
        // 문제 검색 서비스 기준 추천 후보 조회
        return problemSearchService.findProblemPage(
                        1, null, input.getDbmsType(), solveState, input.getCurrentHandle(),
                        solvedCountSort, totalSubmitSort, successSubmitSort,
                        "none", null, null
                )
                .getProblems()
                .stream()
                .limit(input.getCandidateLimit())
                .toList();
    }

    private void addProblemCandidates(Map<String, ProblemListEntry> candidatesByProblemId,
                                      List<ProblemListEntry> problemCandidates) {
        // 중복 문제 제외 후 추천 후보 추가
        for (ProblemListEntry problemCandidate : problemCandidates) {
            candidatesByProblemId.putIfAbsent(problemCandidate.getProblem().getProblemId(), problemCandidate);
        }
    }

    private ProblemRecommendationCandidateOutput toOutput(ProblemListEntry problemEntry) {
        // 추천 문제 후보 응답 변환
        return new ProblemRecommendationCandidateOutput(
                problemEntry.getProblem().getProblemId(), problemEntry.getProblem().getTitle(),
                problemEntry.getProblem().getDbmsType().getValue(), problemEntry.getSolvedUserCount(),
                problemEntry.getTotalSubmitCount(), problemEntry.getSuccessSubmitCount(),
                problemEntry.getSpreadRate(), problemEntry.isSolvedByCurrentUser()
        );
    }

}
