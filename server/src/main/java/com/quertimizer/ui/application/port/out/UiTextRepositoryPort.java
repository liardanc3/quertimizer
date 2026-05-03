package com.quertimizer.ui.application.port.out;

import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;

import java.util.List;
import java.util.Optional;

public interface UiTextRepositoryPort {

    List<UiText> findAllByOrderByIdKeyAscIdLanguageAsc();

    Optional<UiText> findByIdKeyAndIdLanguage(String key, String language);

    Optional<UiText> findById(UiTextId uiTextId);

    UiText save(UiText uiText);

    List<UiText> saveAll(Iterable<UiText> uiTexts);

    boolean existsById(UiTextId uiTextId);

    void delete(UiText uiText);
}
