package com.quertimizer.ui.adapter.in.web.response;

import com.quertimizer.ui.application.output.UiTextOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UiTextRes {

    private final String key;
    private final String value;
    private final String language;
    private final String description;

    public static UiTextRes from(UiTextOutput uiText) {
        return new UiTextRes(
                uiText.getKey(),
                uiText.getValue(),
                uiText.getLanguage(),
                uiText.getDescription()
        );
    }

}
