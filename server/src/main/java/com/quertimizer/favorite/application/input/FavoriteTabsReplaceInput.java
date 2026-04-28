package com.quertimizer.favorite.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class FavoriteTabsReplaceInput {

    private final String userEmail;
    private final List<FavoriteTabInput> tabs;
}
