package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.UiText;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UiTextRes {

    private final String key;
    private final String value;
    private final String language;
    private final String description;

    public static UiTextRes from(UiText uiText) {
        return new UiTextRes(
                uiText.getKey(),
                uiText.getValue(),
                uiText.getLanguage(),
                uiText.getDescription()
        );
    }

}
