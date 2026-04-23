package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.result.ProblemPageResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final double spreadRateMin;
    private final double spreadRateMax;
    private final List<ProblemListItemRes> problems;

    public static ProblemPageRes from(ProblemPageResult result) {
        return new ProblemPageRes(
                result.currentPage(),
                result.pageSize(),
                result.totalCount(),
                result.totalPages(),
                result.spreadRateMin(),
                result.spreadRateMax(),
                result.problems().stream()
                        .map(ProblemListItemRes::from)
                        .toList()
        );
    }

}
