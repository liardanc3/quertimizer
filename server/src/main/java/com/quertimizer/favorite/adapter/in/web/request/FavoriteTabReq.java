package com.quertimizer.favorite.adapter.in.web.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteTabReq {

    @NotBlank(message = "탭 라벨을 입력해 주세요.")
    @Size(max = 200, message = "탭 라벨은 최대 200자까지 입력할 수 있습니다.")
    private String label;

    @NotBlank(message = "탭 경로를 입력해 주세요.")
    @Size(max = 2048, message = "탭 경로는 최대 2048자까지 입력할 수 있습니다.")
    private String path;

    private JsonNode snapshot;
}
