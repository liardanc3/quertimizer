package com.quertimizer.favorite.infrastructure.repository;

import com.quertimizer.favorite.application.port.FavoriteTabRepository;
import com.quertimizer.favorite.domain.entity.FavoriteTab;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteTabJpaRepository extends JpaRepository<FavoriteTab, Long>, FavoriteTabRepository {
}
