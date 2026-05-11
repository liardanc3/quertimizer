package com.quertimizer.favorite.application.input;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class FavoriteTabInput {

    private final String label;
    private final String path;
    private final JsonNode snapshot;
}
