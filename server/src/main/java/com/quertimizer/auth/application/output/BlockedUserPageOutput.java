package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BlockedUserPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<BlockedUserItemOutput> items;
}
