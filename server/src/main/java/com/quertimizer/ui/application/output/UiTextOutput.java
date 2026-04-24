package com.quertimizer.ui.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UiTextOutput {

    private final String key;
    private final String value;
    private final String language;
    private final String description;
}
