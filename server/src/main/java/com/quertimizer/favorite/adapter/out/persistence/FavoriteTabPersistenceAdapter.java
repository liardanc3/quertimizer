package com.quertimizer.favorite.adapter.out.persistence;

import java.util.List;
import com.quertimizer.favorite.application.port.out.FavoriteTabRepositoryPort;
import com.quertimizer.favorite.domain.entity.FavoriteTab;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteTabPersistenceAdapter implements FavoriteTabRepositoryPort {

    private final FavoriteTabJpaRepository favoriteTabJpaRepository;
    private final FavoriteTabPersistenceMapper favoriteTabPersistenceMapper;

    @Override
    public List<FavoriteTab> findAllByUserEmailOrderByDisplayOrderAsc(String userEmail) {
        return favoriteTabJpaRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail).stream()
                .map(favoriteTabPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<FavoriteTab> saveAll(Iterable<FavoriteTab> favoriteTabs) {
        List<FavoriteTabJpaEntity> entities = new java.util.ArrayList<>();
        favoriteTabs.forEach(favoriteTab -> entities.add(favoriteTabPersistenceMapper.toEntity(favoriteTab)));
        return favoriteTabJpaRepository.saveAll(entities).stream()
                .map(favoriteTabPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByUserEmail(String userEmail) {
        favoriteTabJpaRepository.deleteByUserEmail(userEmail);
    }
}
