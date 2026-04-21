package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminAnomalyTrendItemRes {

    private final String userId;
    private final String actionType;
    private final long count;

}
