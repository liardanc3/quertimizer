package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
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
