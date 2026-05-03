package com.quertimizer.favorite.application.port.out;

import com.quertimizer.favorite.domain.entity.FavoriteTab;

import java.util.List;

public interface FavoriteTabRepositoryPort {

    List<FavoriteTab> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail);

    List<FavoriteTab> saveAll(Iterable<FavoriteTab> favoriteTabs);

    void deleteByUserEmail(String userEmail);
}
