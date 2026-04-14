package com.quertimizer.endpoint.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UiTextSaveReq {

    @NotBlank
    @Size(max = 100)
    private String key;

    @NotBlank
    private String value;

    @NotBlank
    @Size(max = 20)
    private String language;

    @NotBlank
    private String description;

}
