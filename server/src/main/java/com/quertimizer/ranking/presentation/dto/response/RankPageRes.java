package com.quertimizer.ranking.presentation.dto.response;

import com.quertimizer.ranking.application.result.RankPageResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RankPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<RankListItemRes> ranks;

    public static RankPageRes from(RankPageResult result) {
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
