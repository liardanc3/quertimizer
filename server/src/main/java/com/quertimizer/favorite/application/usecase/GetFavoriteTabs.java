package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.service.FavoriteTabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetFavoriteTabs {

    private final FavoriteTabService favoriteTabService;

    public FavoriteTabsOutput execute(String userEmail) {
        // 즐겨찾기 탭 목록을 조회
        return favoriteTabService.getFavoriteTabs(userEmail);
    }
}
