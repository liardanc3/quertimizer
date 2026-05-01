package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.port.FavoriteTabRepository;
import com.quertimizer.favorite.application.support.FavoriteTabSnapshotSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetFavoriteTabs {

    private final FavoriteTabRepository favoriteTabRepository;
    private final FavoriteTabSnapshotSupport favoriteTabSnapshotSupport;

    /**
     * 즐겨찾기 탭 목록을 조회한다.
     *
     * @param userEmail 즐겨찾기를 조회할 사용자 이메일
     */
    @Transactional(readOnly = true)
    public FavoriteTabsOutput execute(String userEmail) {
        return new FavoriteTabsOutput(favoriteTabRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail).stream()
                .map(tab -> new FavoriteTabOutput(
                        tab.getLabel(), tab.getPath(),
                        favoriteTabSnapshotSupport.deserialize(tab.getSnapshotJson())
                ))
                .toList());
    }
}
