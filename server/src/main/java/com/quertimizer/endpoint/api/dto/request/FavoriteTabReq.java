package com.quertimizer.endpoint.api.dto.request;

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

    @NotBlank(message = "즐겨찾기 이름은 비어 있을 수 없습니다.")
    @Size(max = 200, message = "즐겨찾기 이름은 최대 200자까지 가능합니다.")
    private String label;

    @NotBlank(message = "즐겨찾기 경로는 비어 있을 수 없습니다.")
    @Size(max = 2048, message = "즐겨찾기 경로는 최대 2048자까지 가능합니다.")
    private String path;

    private JsonNode snapshot;
}
