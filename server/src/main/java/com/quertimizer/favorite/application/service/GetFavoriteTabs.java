package com.quertimizer.favorite.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.favorite.application.port.in.GetFavoriteTabsUseCase;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.port.out.FavoriteTabRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetFavoriteTabs implements GetFavoriteTabsUseCase {

    private final FavoriteTabRepositoryPort favoriteTabRepository;
    private final FavoriteService favoriteService;

    /**
     * 즐겨찾기 탭 목록을 조회한다.
     *
     * @param userEmail 즐겨찾기를 조회할 사용자 이메일
     */
    @Transactional(readOnly = true)
    @Override
    @Log("즐겨찾기 탭 조회")
    public FavoriteTabsOutput execute(String userEmail) {
        return new FavoriteTabsOutput(
                favoriteTabRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail)
                        .stream()
                        .map(tab ->
                                new FavoriteTabOutput(tab.getLabel(), tab.getPath(), favoriteService.deserialize(tab.getSnapshotJson()))
                        )
                        .toList()
        );
    }
}
