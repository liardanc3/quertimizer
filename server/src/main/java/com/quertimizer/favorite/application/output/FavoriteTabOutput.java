package com.quertimizer.favorite.application.output;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteTabOutput {

    private final String label;
    private final String path;
    private final JsonNode snapshot;
}
