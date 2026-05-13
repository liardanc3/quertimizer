package com.quertimizer.favorite.adapter.in.http.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import lombok.Data;

@Data
public class FavoriteTabRes {

    private final String label;
    private final String path;
    private final JsonNode snapshot;

    public static FavoriteTabRes from(FavoriteTabOutput result) {
        return new FavoriteTabRes(result.getLabel(), result.getPath(), result.getSnapshot());
    }
}
