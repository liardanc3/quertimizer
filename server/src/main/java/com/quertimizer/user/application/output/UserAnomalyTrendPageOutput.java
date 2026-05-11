package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserAnomalyTrendPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<UserAnomalyTrendItemOutput> items;
}
