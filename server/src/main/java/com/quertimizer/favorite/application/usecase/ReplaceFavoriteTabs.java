package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.input.FavoriteTabsReplaceInput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.service.FavoriteTabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplaceFavoriteTabs {

    private final FavoriteTabService favoriteTabService;

    /**
     * 즐겨찾기 탭 목록을 교체한다.
     *
     * @param input 즐겨찾기 탭 소유자와 교체할 탭 목록 입력
     */
    public FavoriteTabsOutput execute(FavoriteTabsReplaceInput input) {
        return favoriteTabService.replaceFavoriteTabs(input.getUserEmail(), input.getTabs());
    }
}
