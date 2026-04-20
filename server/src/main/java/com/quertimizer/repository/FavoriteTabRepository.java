package com.quertimizer.repository;

import com.quertimizer.entity.FavoriteTab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteTabRepository extends JpaRepository<FavoriteTab, Long> {

    List<FavoriteTab> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail);

    void deleteByUserEmail(String userEmail);
}
