package com.quertimizer.user.domain.model;

import lombok.Data;

@Data
public class UserAnomalySubmitTrend {

    private final String handle;
    private final long submitCount;

}
