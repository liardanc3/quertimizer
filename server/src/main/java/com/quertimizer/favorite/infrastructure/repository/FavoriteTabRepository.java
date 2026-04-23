package com.quertimizer.favorite.infrastructure.repository;

import com.quertimizer.favorite.domain.entity.FavoriteTab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteTabRepository extends JpaRepository<FavoriteTab, Long> {

    List<FavoriteTab> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail);

    void deleteByUserEmail(String userEmail);
}
