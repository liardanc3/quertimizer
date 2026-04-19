package com.quertimizer.endpoint.api.dto.response;

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

}
