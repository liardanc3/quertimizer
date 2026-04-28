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

    /**
     * UI 텍스트를 생성한다.
     *
     * @param input 생성할 UI 텍스트 입력
     */
    public UiTextOutput execute(UiTextInput input) {
        return uiTextService.createUiText(input);
    }
}
