package com.quertimizer.favorite.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.quertimizer.favorite.application.input.FavoriteTabInput;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.domain.model.FavoriteFailReason;
import com.quertimizer.favorite.domain.entity.FavoriteTab;
import com.quertimizer.favorite.application.port.FavoriteTabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteTabService {

    private final FavoriteTabRepository favoriteTabRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public FavoriteTabsOutput getFavoriteTabs(String userEmail) {
        // 즐겨찾기 탭 목록을 조회
        List<FavoriteTabOutput> tabs = favoriteTabRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail).stream()
                .map(this::toFavoriteTabOutput)
                .toList();

        return new FavoriteTabsOutput(tabs);
    }

    public FavoriteTabsOutput replaceFavoriteTabs(String userEmail, List<FavoriteTabInput> tabs) {
        // 즐겨찾기 탭 목록을 교체
        favoriteTabRepository.deleteByUserEmail(userEmail);

        List<FavoriteTab> nextTabs = new ArrayList<>();
        int nextDisplayOrder = 0;
        for (FavoriteTabInput tab : tabs) {
            nextTabs.add(FavoriteTab.create(
                    userEmail,
                    nextDisplayOrder++,
                    tab.getLabel().trim(),
                    tab.getPath().trim(),
                    serializeSnapshot(tab.getSnapshot())
            ));
        }

        if (!nextTabs.isEmpty()) {
            favoriteTabRepository.saveAll(nextTabs);
        }

        return new FavoriteTabsOutput(nextTabs.stream().map(this::toFavoriteTabOutput).toList());
    }

    private FavoriteTabOutput toFavoriteTabOutput(FavoriteTab favoriteTab) {
        // 즐겨찾기 탭 응답으로 변환
        return new FavoriteTabOutput(
                favoriteTab.getLabel(),
                favoriteTab.getPath(),
                deserializeSnapshot(favoriteTab.getSnapshotJson())
        );
    }

    private String serializeSnapshot(JsonNode snapshot) {
        // 스냅샷 직렬화
        if (snapshot == null || snapshot.isNull()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(FavoriteFailReason.SNAPSHOT_SERIALIZE_FAILED.getMessage(), exception);
        }
    }

    private JsonNode deserializeSnapshot(String snapshotJson) {
        // 스냅샷 역직렬화
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return NullNode.getInstance();
        }

        try {
            return objectMapper.readTree(snapshotJson);
        } catch (JsonProcessingException exception) {
            return NullNode.getInstance();
        }
    }
}
