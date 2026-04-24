package com.quertimizer.favorite.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FavoriteTabsOutput {

    private final List<FavoriteTabOutput> tabs;
}
