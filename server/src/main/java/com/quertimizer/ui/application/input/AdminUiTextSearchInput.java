package com.quertimizer.ui.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AdminUiTextSearchInput {

    private final int page;
    private final Integer pageSize;
    private final String query;
}
