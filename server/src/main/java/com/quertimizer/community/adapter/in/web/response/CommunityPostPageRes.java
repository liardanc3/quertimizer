package com.quertimizer.community.adapter.in.web.response;

import com.quertimizer.community.application.output.CommunityPostPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<CommunityPostSummaryRes> posts;

    public static CommunityPostPageRes from(CommunityPostPageOutput result) {
        return new CommunityPostPageRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getPosts().stream()
                        .map(CommunityPostSummaryRes::from)
                        .toList()
        );
    }
}
