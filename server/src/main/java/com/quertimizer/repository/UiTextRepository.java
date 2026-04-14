package com.quertimizer.repository;

import com.quertimizer.entity.UiText;
import com.quertimizer.entity.UiTextId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UiTextRepository extends JpaRepository<UiText, UiTextId> {

    List<UiText> findAllByOrderByIdKeyAscIdLanguageAsc();

    Optional<UiText> findByIdKeyAndIdLanguage(String key, String language);

}
