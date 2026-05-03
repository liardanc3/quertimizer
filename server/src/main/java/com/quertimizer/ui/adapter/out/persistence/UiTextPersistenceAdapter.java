package com.quertimizer.ui.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UiTextPersistenceAdapter implements UiTextRepositoryPort {

    private final UiTextJpaRepository uiTextJpaRepository;
    private final UiTextPersistenceMapper uiTextPersistenceMapper;

    @Override
    public List<UiText> findAllByOrderByIdKeyAscIdLanguageAsc() {
        return uiTextJpaRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .map(uiTextPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UiText> findByIdKeyAndIdLanguage(String key, String language) {
        return uiTextJpaRepository.findByIdKeyAndIdLanguage(key, language)
                .map(uiTextPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UiText> findById(UiTextId uiTextId) {
        return uiTextJpaRepository.findById(toJpaId(uiTextId))
                .map(uiTextPersistenceMapper::toDomain);
    }

    @Override
    public UiText save(UiText uiText) {
        UiTextJpaEntity savedEntity = uiTextJpaRepository.findById(toJpaId(uiText.getId()))
                .map(entity -> {
                    uiTextPersistenceMapper.updateEntity(entity, uiText);
                    return entity;
                })
                .orElseGet(() -> uiTextPersistenceMapper.toEntity(uiText));
        return uiTextPersistenceMapper.toDomain(uiTextJpaRepository.save(savedEntity));
    }

    @Override
    public List<UiText> saveAll(Iterable<UiText> uiTexts) {
        List<UiTextJpaEntity> entities = new java.util.ArrayList<>();
        uiTexts.forEach(uiText -> entities.add(uiTextPersistenceMapper.toEntity(uiText)));
        return uiTextJpaRepository.saveAll(entities).stream()
                .map(uiTextPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UiTextId uiTextId) {
        return uiTextJpaRepository.existsById(toJpaId(uiTextId));
    }

    @Override
    public void delete(UiText uiText) {
        uiTextJpaRepository.deleteById(toJpaId(uiText.getId()));
    }

    private UiTextJpaId toJpaId(UiTextId uiTextId) {
        // UI 텍스트 도메인 식별자를 JPA 식별자로 변환
        return UiTextJpaId.create(uiTextId.getKey(), uiTextId.getLanguage());
    }
}
