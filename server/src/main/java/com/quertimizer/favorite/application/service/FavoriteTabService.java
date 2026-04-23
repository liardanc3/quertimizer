package com.quertimizer.favorite.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.quertimizer.favorite.domain.model.FavoriteFailReason;
import com.quertimizer.favorite.presentation.dto.request.FavoriteTabReq;
import com.quertimizer.favorite.presentation.dto.request.FavoriteTabsUpdateReq;
import com.quertimizer.favorite.presentation.dto.response.FavoriteTabRes;
import com.quertimizer.favorite.presentation.dto.response.FavoriteTabsRes;
import com.quertimizer.favorite.domain.entity.FavoriteTab;
import com.quertimizer.favorite.infrastructure.repository.FavoriteTabRepository;
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
    public FavoriteTabsRes getFavoriteTabs(String userEmail) {
        List<FavoriteTabRes> tabs = favoriteTabRepository.findAllByUserEmailOrderByDisplayOrderAsc(userEmail).stream()
                .map(this::toResponse)
                .toList();

        return new FavoriteTabsRes(tabs);
    }

    public FavoriteTabsRes replaceFavoriteTabs(String userEmail, FavoriteTabsUpdateReq request) {
        favoriteTabRepository.deleteByUserEmail(userEmail);

        List<FavoriteTab> nextTabs = new ArrayList<>();
        int nextDisplayOrder = 0;
        for (FavoriteTabReq tab : request.getTabs()) {
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

        return new FavoriteTabsRes(nextTabs.stream().map(this::toResponse).toList());
    }

    private FavoriteTabRes toResponse(FavoriteTab favoriteTab) {
        return new FavoriteTabRes(
                favoriteTab.getLabel(),
                favoriteTab.getPath(),
                deserializeSnapshot(favoriteTab.getSnapshotJson())
        );
    }

    private String serializeSnapshot(JsonNode snapshot) {
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
