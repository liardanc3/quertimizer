package com.quertimizer.user.application.output;

import lombok.Data;

@Data
public class UserAnomalyTrendItemOutput {

    private final String handle;
    private final String actionType;
    private final long count;
}
