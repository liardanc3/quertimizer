package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.BlockedUserPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class BlockedUserPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<BlockedUserItemRes> items;

    public static BlockedUserPageRes from(BlockedUserPageOutput result) {
        return new BlockedUserPageRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getItems().stream()
                        .map(BlockedUserItemRes::from)
                        .toList()
        );
    }
}
