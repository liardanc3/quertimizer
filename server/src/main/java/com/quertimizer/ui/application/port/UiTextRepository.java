package com.quertimizer.ui.application.port;

import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;

import java.util.List;
import java.util.Optional;

public interface UiTextRepository {

    List<UiText> findAllByOrderByIdKeyAscIdLanguageAsc();

    Optional<UiText> findByIdKeyAndIdLanguage(String key, String language);

    Optional<UiText> findById(UiTextId uiTextId);

    <S extends UiText> S save(S uiText);

    <S extends UiText> List<S> saveAll(Iterable<S> uiTexts);

    boolean existsById(UiTextId uiTextId);

    void delete(UiText uiText);
}
