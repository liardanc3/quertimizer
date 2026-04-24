package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAnomalyTrendItemOutput {

    private final String handle;
    private final String actionType;
    private final long count;
}
