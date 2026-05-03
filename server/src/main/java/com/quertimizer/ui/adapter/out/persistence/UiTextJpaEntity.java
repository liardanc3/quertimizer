package com.quertimizer.ui.adapter.out.persistence;

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
public class UiTextJpaEntity {

    @EmbeddedId
    private UiTextJpaId id;

    @Column(name = "\"value\"", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    public static UiTextJpaEntity create(String key, String value, String language, String description) {
        // UI 텍스트 JPA 엔티티 생성
        return new UiTextJpaEntity(UiTextJpaId.create(key, language), value, description);
    }

    public String getKey() {
        // UI 텍스트 key 조회
        return id.getKey();
    }

    public String getLanguage() {
        // UI 텍스트 language 조회
        return id.getLanguage();
    }

    public void update(String value, String description) {
        // UI 텍스트 내용 변경
        this.value = value;
        this.description = description;
    }

    private UiTextJpaEntity(UiTextJpaId id, String value, String description) {
        this.id = id;
        this.value = value;
        this.description = description;
    }
}
