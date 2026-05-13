package com.quertimizer.favorite.adapter.in.http.response;

import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteTabsRes {

    private final List<FavoriteTabRes> tabs;

    public static FavoriteTabsRes from(FavoriteTabsOutput result) {
        return new FavoriteTabsRes(result.getTabs().stream()
                .map(FavoriteTabRes::from)
                .toList());
    }
}
