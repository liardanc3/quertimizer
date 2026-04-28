package com.quertimizer.ui.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateUiTextInput {

    private final String key;
    private final String language;
    private final UiTextInput text;
}
