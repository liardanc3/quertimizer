package com.quertimizer.favorite.presentation.dto.request;

import com.quertimizer.favorite.application.input.FavoriteTabInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteTabsUpdateReq {

    @Valid
    @NotNull
    @Size(max = 10, message = "즐겨찾기 탭은 최대 10개까지 저장할 수 있습니다.")
    @Builder.Default
    private List<FavoriteTabReq> tabs = new ArrayList<>();

    public List<FavoriteTabInput> toFavoriteTabInputs() {
        return tabs.stream()
                .map(tab -> new FavoriteTabInput(tab.getLabel(), tab.getPath(), tab.getSnapshot()))
                .toList();
    }
}
