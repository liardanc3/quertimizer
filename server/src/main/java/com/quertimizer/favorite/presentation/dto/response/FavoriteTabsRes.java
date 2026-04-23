package com.quertimizer.favorite.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FavoriteTabsRes {

    private final List<FavoriteTabRes> tabs;
}
