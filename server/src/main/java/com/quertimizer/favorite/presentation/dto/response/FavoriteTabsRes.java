package com.quertimizer.favorite.presentation.dto.response;

import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FavoriteTabsRes {

    private final List<FavoriteTabRes> tabs;

    public static FavoriteTabsRes from(FavoriteTabsOutput result) {
        return new FavoriteTabsRes(result.getTabs().stream()
                .map(FavoriteTabRes::from)
                .toList());
    }
}
