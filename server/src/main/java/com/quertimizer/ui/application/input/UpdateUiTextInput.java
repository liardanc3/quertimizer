package com.quertimizer.ui.application.input;

import lombok.Data;

@Data
public class UpdateUiTextInput {

    private final String key;
    private final String language;
    private final UiTextInput text;
}
