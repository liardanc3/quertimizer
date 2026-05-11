package com.quertimizer.auth.application.output;

import lombok.Data;

import java.util.List;

@Data
public class BlockedUserPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<BlockedUserItemOutput> items;
}
