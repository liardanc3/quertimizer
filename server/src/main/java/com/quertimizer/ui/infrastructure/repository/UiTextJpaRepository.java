package com.quertimizer.ui.infrastructure.repository;

import com.quertimizer.ui.application.port.UiTextRepository;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiTextJpaRepository extends JpaRepository<UiText, UiTextId>, UiTextRepository {
}
