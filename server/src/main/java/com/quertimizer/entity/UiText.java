package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ui_text")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UiText {

    @EmbeddedId
    private UiTextId id;

    @Column(name = "\"value\"", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    public static UiText create(String key, String value, String language, String description) {
        return new UiText(UiTextId.create(key, language), value, description);
    }

    public String getKey() {
        return id.getKey();
    }

    public String getLanguage() {
        return id.getLanguage();
    }

    public void changeContent(String value, String description) {
        this.value = value;
        this.description = description;
    }

    private UiText(UiTextId id, String value, String description) {
        this.id = id;
        this.value = value;
        this.description = description;
    }

}
