package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.response.ProblemDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemListItemRes;
import com.quertimizer.endpoint.api.dto.response.ProblemPageRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSubmittedHistoryRes;
import com.quertimizer.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemStore problemStore;

    public ProblemPageRes getProblems(int page,
                                      String query,
                                      String solveState,
                                      String currentUserId,
                                      String solvedCountSort) {

        // 조회 조건 반영 후 메모리 페이지 조회
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(
                page,
                query,
                solveState,
                currentUserId,
                "asc".equalsIgnoreCase(solvedCountSort)
        );

        // 목록 응답 DTO 변환
        List<ProblemListItemRes> problems = problemPage.problems().stream()
                .map(problemEntry -> ProblemListItemRes.of(
                        problemEntry.problem(),
                        problemEntry.submittedHistories().stream()
                                .map(ProblemSubmittedHistoryRes::from)
                                .toList()
                ))
                .toList();

        return new ProblemPageRes(
                problemPage.currentPage(),
                problemPage.pageSize(),
                problemPage.totalCount(),
                problemPage.totalPages(),
                problems
        );
    }

    public Optional<ProblemDetailRes> getProblem(String problemId) {

        // 문제 기본 정보 조회
        return problemStore.findProblem(problemId)
                .map(ProblemDetailRes::from);
    }

}
