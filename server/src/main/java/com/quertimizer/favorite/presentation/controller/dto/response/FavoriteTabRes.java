package com.quertimizer.favorite.presentation.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteTabRes {

    private final String label;
    private final String path;
    private final JsonNode snapshot;

    public static FavoriteTabRes from(FavoriteTabOutput result) {
        return new FavoriteTabRes(result.getLabel(), result.getPath(), result.getSnapshot());
    }
}
