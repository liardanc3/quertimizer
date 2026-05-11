package com.quertimizer.ui.application.input;

import lombok.Data;

@Data
public class AdminUiTextSearchInput {

    private final int page;
    private final Integer pageSize;
    private final String query;
}
