package com.quertimizer.favorite.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteTabJpaRepository extends JpaRepository<FavoriteTabJpaEntity, Long> {
    List<FavoriteTabJpaEntity> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail);
    void deleteByUserEmail(String userEmail);
}
