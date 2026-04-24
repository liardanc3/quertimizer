package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.input.FavoriteTabInput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.service.FavoriteTabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReplaceFavoriteTabs {

    private final FavoriteTabService favoriteTabService;

    public FavoriteTabsOutput execute(String userEmail, List<FavoriteTabInput> tabs) {
        // 즐겨찾기 탭 목록을 교체
        return favoriteTabService.replaceFavoriteTabs(userEmail, tabs);
    }
}
