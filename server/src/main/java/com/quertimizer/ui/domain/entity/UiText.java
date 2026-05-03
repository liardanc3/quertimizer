package com.quertimizer.ui.domain.entity;

import com.quertimizer.ui.domain.entity.ids.UiTextId;
import lombok.Getter;

@Getter
public class UiText {

    private UiTextId id;
    private String value;
    private String description;

    public static UiText create(String key, String value, String language, String description) {
        // UI 텍스트 생성
        return new UiText(UiTextId.create(key, language), value, description);
    }

    public static UiText restore(String key, String value, String language, String description) {
        // 저장된 UI 텍스트 상태 복원
        return new UiText(UiTextId.create(key, language), value, description);
    }

    public String getKey() {
        // 키 조회
        return id.getKey();
    }

    public String getLanguage() {
        // 언어 조회
        return id.getLanguage();
    }

    public void changeContent(String value, String description) {
        // UI 텍스트 본문 변경
        this.value = value;
        this.description = description;
    }

    private UiText(UiTextId id, String value, String description) {
        this.id = id;
        this.value = value;
        this.description = description;
    }

}
