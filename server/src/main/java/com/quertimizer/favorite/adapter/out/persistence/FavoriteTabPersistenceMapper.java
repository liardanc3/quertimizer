package com.quertimizer.favorite.adapter.out.persistence;

import com.quertimizer.favorite.domain.entity.FavoriteTab;
import org.springframework.stereotype.Component;

@Component
public class FavoriteTabPersistenceMapper {

    public FavoriteTab toDomain(FavoriteTabJpaEntity entity) {
        // 즐겨찾기 탭 JPA 엔티티를 도메인 엔티티로 변환
        return FavoriteTab.restore(
                entity.getFavoriteTabId(), entity.getUserEmail(),
                entity.getDisplayOrder(), entity.getLabel(),
                entity.getPath(), entity.getSnapshotJson()
        );
    }

    public FavoriteTabJpaEntity toEntity(FavoriteTab favoriteTab) {
        // 즐겨찾기 탭 도메인 엔티티를 JPA 엔티티로 변환
        return FavoriteTabJpaEntity.create(
                favoriteTab.getUserEmail(), favoriteTab.getDisplayOrder(),
                favoriteTab.getLabel(), favoriteTab.getPath(), favoriteTab.getSnapshotJson()
        );
    }
}
