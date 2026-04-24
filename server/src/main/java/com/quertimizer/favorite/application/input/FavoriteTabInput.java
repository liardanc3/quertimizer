package com.quertimizer.favorite.application.input;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteTabInput {

    private final String label;
    private final String path;
    private final JsonNode snapshot;
}
