package com.quertimizer.favorite.presentation.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.favorite.domain.model.FavoriteValidationMessage.LABEL_LENGTH_EXCEEDED;
import static com.quertimizer.favorite.domain.model.FavoriteValidationMessage.LABEL_REQUIRED;
import static com.quertimizer.favorite.domain.model.FavoriteValidationMessage.PATH_LENGTH_EXCEEDED;
import static com.quertimizer.favorite.domain.model.FavoriteValidationMessage.PATH_REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteTabReq {

    @NotBlank(message = LABEL_REQUIRED)
    @Size(max = 200, message = LABEL_LENGTH_EXCEEDED)
    private String label;

    @NotBlank(message = PATH_REQUIRED)
    @Size(max = 2048, message = PATH_LENGTH_EXCEEDED)
    private String path;

    private JsonNode snapshot;
}
