package com.quertimizer.ui.adapter.in.web.request;

import com.quertimizer.ui.application.input.UiTextInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

    public UiTextInput toUiTextInput() {
        return new UiTextInput(key, value, language, description);
    }
}
