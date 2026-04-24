package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.UiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateUiText {

    private final UiTextService uiTextService;

    public UiTextOutput execute(UiTextInput input) {
        // UI 텍스트를 생성
        return uiTextService.createUiText(input);
    }
}
