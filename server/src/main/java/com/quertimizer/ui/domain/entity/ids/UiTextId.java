package com.quertimizer.ui.domain.entity.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UiTextId implements Serializable {

    @Column(name = "\"key\"", nullable = false, length = 100)
    private String key;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    public static UiTextId create(String key, String language) {
        // UI 텍스트 식별자 생성
        return new UiTextId(key, language);
    }

}
