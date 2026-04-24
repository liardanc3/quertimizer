package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserAnomalyTrendPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<UserAnomalyTrendItemOutput> items;
}
