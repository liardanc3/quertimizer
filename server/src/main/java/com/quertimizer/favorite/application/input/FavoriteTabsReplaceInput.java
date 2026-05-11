package com.quertimizer.favorite.application.input;

import lombok.Data;

import java.util.List;

@Data
public class FavoriteTabsReplaceInput {

    private final String userEmail;
    private final List<FavoriteTabInput> tabs;
}
