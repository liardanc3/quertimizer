package com.quertimizer.ui.adapter.out.persistence;

import com.quertimizer.ui.domain.entity.UiText;
import org.springframework.stereotype.Component;

@Component
public class UiTextPersistenceMapper {

    public UiText toDomain(UiTextJpaEntity entity) {
        // UI 텍스트 JPA 엔티티를 도메인 엔티티로 변환
        return UiText.restore(entity.getKey(), entity.getValue(), entity.getLanguage(), entity.getDescription());
    }

    public UiTextJpaEntity toEntity(UiText uiText) {
        // UI 텍스트 도메인 엔티티를 JPA 엔티티로 변환
        return UiTextJpaEntity.create(
                uiText.getKey(), uiText.getValue(),
                uiText.getLanguage(), uiText.getDescription()
        );
    }

    public void updateEntity(UiTextJpaEntity entity, UiText uiText) {
        // UI 텍스트 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(uiText.getValue(), uiText.getDescription());
    }
}
