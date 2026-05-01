package com.quertimizer.favorite.application.usecase;

import com.quertimizer.favorite.application.input.FavoriteTabInput;
import com.quertimizer.favorite.application.input.FavoriteTabsReplaceInput;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.application.port.FavoriteTabRepository;
import com.quertimizer.favorite.application.support.FavoriteTabSnapshotSupport;
import com.quertimizer.favorite.domain.entity.FavoriteTab;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReplaceFavoriteTabs {

    private final FavoriteTabRepository favoriteTabRepository;
    private final FavoriteTabSnapshotSupport favoriteTabSnapshotSupport;

    /**
     * 즐겨찾기 탭 목록을 교체한다.
     *
     * <ol>
     *   <li>기존 즐겨찾기 탭 삭제
     *   <li>입력 탭 엔티티 변환
     *   <li>신규 즐겨찾기 탭 저장과 응답 변환
     * </ol>
     *
     * @param input 즐겨찾기 탭 소유자와 교체할 탭 목록 입력
     */
    @Transactional
    public FavoriteTabsOutput execute(FavoriteTabsReplaceInput input) {
        favoriteTabRepository.deleteByUserEmail(input.getUserEmail());

        List<FavoriteTab> nextTabs = new ArrayList<>();
        int nextDisplayOrder = 0;
        for (FavoriteTabInput tab : input.getTabs()) {
            nextTabs.add(FavoriteTab.create(
                    input.getUserEmail(), nextDisplayOrder++,
                    tab.getLabel().trim(), tab.getPath().trim(),
                    favoriteTabSnapshotSupport.serialize(tab.getSnapshot())
            ));
        }

        if (!nextTabs.isEmpty()) {
            favoriteTabRepository.saveAll(nextTabs);
        }

        return new FavoriteTabsOutput(nextTabs.stream()
                .map(tab -> new FavoriteTabOutput(
                        tab.getLabel(), tab.getPath(),
                        favoriteTabSnapshotSupport.deserialize(tab.getSnapshotJson())
                ))
                .toList());
    }
}
