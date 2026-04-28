package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.UpdateUiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUiText {

    private final UiTextService uiTextService;

    /**
     * UI 텍스트를 수정한다.
     *
     * @param input 수정할 UI 텍스트 key, 언어, 내용 입력
     */
    public UiTextOutput execute(UpdateUiTextInput input) {
        return uiTextService.updateUiText(input.getKey(), input.getLanguage(), input.getText());
    }
}
