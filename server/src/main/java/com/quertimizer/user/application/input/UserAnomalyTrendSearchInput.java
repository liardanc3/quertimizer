package com.quertimizer.user.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAnomalyTrendSearchInput {

    private final String range;
    private final String startedAt;
    private final String endedAt;
    private final int page;
    private final Integer pageSize;
}
