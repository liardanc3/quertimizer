package com.quertimizer.submit.presentation.dto.response;

import com.quertimizer.submit.application.output.SubmitHistoryPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SubmitHistoryPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<String> problemIds;
    private final List<SubmitHistoryListItemRes> histories;

    public static SubmitHistoryPageRes from(SubmitHistoryPageOutput result) {
        return new SubmitHistoryPageRes(
                result.currentPage(),
                result.pageSize(),
                result.totalCount(),
                result.totalPages(),
                result.problemIds(),
                result.histories().stream()
                        .map(SubmitHistoryListItemRes::from)
                        .toList()
        );
    }

}
