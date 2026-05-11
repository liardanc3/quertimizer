package com.quertimizer.user.application.input;

import lombok.Data;

@Data
public class UserAnomalyTrendSearchInput {

    private final String range;
    private final String startedAt;
    private final String endedAt;
    private final int page;
    private final Integer pageSize;
}
