package com.quertimizer.favorite.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.quertimizer.favorite.domain.model.FavoriteValidationMessage.TAB_LIMIT_EXCEEDED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteTabsUpdateReq {

    @Valid
    @NotNull
    @Size(max = 10, message = TAB_LIMIT_EXCEEDED)
    @Builder.Default
    private List<FavoriteTabReq> tabs = new ArrayList<>();
}
