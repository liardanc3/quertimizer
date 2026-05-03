package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.SubmitHistoryPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SubmitHistoryPageResponse {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<String> problemIds;
    private final List<SubmitHistoryListItemResponse> histories;

    public static SubmitHistoryPageResponse from(SubmitHistoryPageOutput result) {
        return new SubmitHistoryPageResponse(
                result.currentPage(),
                result.pageSize(),
                result.totalCount(),
                result.totalPages(),
                result.problemIds(),
                result.histories().stream()
                        .map(SubmitHistoryListItemResponse::from)
                        .toList()
        );
    }

}
