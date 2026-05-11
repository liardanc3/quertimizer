package com.quertimizer.ranking.adapter.in.web.response;

import com.quertimizer.ranking.application.output.RankPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class RankPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<RankListItemRes> ranks;

    public static RankPageRes from(RankPageOutput result) {
        return new RankPageRes(
                result.currentPage(),
                result.pageSize(),
                result.totalCount(),
                result.totalPages(),
                result.ranks().stream()
                        .map(RankListItemRes::from)
                        .toList()
        );
    }

}
