package com.quertimizer.endpoint.api.dto.request;

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
    @Size(max = 10, message = "즐겨찾기는 최대 10개까지 저장할 수 있습니다.")
    @Builder.Default
    private List<FavoriteTabReq> tabs = new ArrayList<>();
}
