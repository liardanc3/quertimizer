package com.quertimizer.favorite.application.port;

import com.quertimizer.favorite.domain.entity.FavoriteTab;

import java.util.List;

public interface FavoriteTabRepository {

    List<FavoriteTab> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail);

    <S extends FavoriteTab> List<S> saveAll(Iterable<S> favoriteTabs);

    void deleteByUserEmail(String userEmail);
}
