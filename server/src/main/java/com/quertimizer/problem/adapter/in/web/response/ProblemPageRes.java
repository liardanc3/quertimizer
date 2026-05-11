package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.ProblemPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class ProblemPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final double spreadRateMin;
    private final double spreadRateMax;
    private final List<ProblemListItemRes> problems;

    public static ProblemPageRes from(ProblemPageOutput result) {
        return new ProblemPageRes(
                result.currentPage(), result.pageSize(), result.totalCount(), result.totalPages(),
                result.spreadRateMin(), result.spreadRateMax(),
                result.problems().stream()
                        .map(ProblemListItemRes::from)
                        .toList()
        );
    }

}
