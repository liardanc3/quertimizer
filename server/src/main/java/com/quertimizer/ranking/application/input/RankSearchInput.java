package com.quertimizer.ranking.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RankSearchInput {

    private final int requestedPage;
    private final Integer requestedPageSize;
    private final String dbms;
    private final String query;
    private final String sortKey;
}
