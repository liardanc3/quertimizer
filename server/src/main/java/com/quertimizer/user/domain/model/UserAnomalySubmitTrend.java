package com.quertimizer.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAnomalySubmitTrend {

    private final String handle;
    private final long submitCount;

}
