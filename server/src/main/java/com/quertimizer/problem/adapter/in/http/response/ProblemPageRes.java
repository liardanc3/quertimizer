package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class ProblemPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<ProblemListItemRes> problems;

    public static ProblemPageRes from(ProblemPageOutput result) {
        return new ProblemPageRes(
                result.currentPage(), result.pageSize(), result.totalCount(), result.totalPages(),
                result.problems().stream()
                        .map(ProblemListItemRes::from)
                        .toList()
        );
    }

}
