package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.service.FavoriteTabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetFavoriteTabs {

    private final FavoriteTabService favoriteTabService;

    /**
     * 즐겨찾기 탭 목록을 조회한다.
     *
     * @param userEmail 즐겨찾기를 조회할 사용자 이메일
     */
    public FavoriteTabsOutput execute(String userEmail) {
        return favoriteTabService.getFavoriteTabs(userEmail);
    }
}
