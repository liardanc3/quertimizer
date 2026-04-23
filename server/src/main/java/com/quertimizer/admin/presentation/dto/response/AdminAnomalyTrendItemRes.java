package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminAnomalyTrendItemRes {

    private final String handle;
    private final String actionType;
    private final long count;

}
