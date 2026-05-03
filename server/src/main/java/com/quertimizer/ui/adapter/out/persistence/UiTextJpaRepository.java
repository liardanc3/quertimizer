package com.quertimizer.ui.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiTextJpaRepository extends JpaRepository<UiTextJpaEntity, UiTextJpaId> {
    List<UiTextJpaEntity> findAllByOrderByIdKeyAscIdLanguageAsc();
    Optional<UiTextJpaEntity> findByIdKeyAndIdLanguage(String key, String language);
}
