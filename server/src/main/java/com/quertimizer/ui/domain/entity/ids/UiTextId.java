package com.quertimizer.ui.domain.entity.ids;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UiTextId implements Serializable {

    private String key;
    private String language;

    public static UiTextId create(String key, String language) {
        // UI 텍스트 식별자 생성
        return new UiTextId(key, language);
    }

}
