package com.quertimizer.endpoint.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteTabRes {

    private final String label;
    private final String path;
    private final JsonNode snapshot;
}
