package com.quertimizer.favorite.application.output;

import lombok.Data;

import java.util.List;

@Data
public class FavoriteTabsOutput {

    private final List<FavoriteTabOutput> tabs;
}
