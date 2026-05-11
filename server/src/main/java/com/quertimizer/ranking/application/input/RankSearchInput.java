package com.quertimizer.ranking.application.input;

import lombok.Data;

@Data
public class RankSearchInput {

    private final int requestedPage;
    private final Integer requestedPageSize;
    private final String dbms;
    private final String query;
    private final String sortKey;
}
