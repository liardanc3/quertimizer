package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.BlockedIpPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class BlockedIpPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<BlockedIpItemRes> items;

    public static BlockedIpPageRes from(BlockedIpPageOutput result) {
        return new BlockedIpPageRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getItems().stream()
                        .map(BlockedIpItemRes::from)
                        .toList()
        );
    }
}
