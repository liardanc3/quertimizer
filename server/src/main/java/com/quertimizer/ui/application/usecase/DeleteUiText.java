package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteUiText {

    private final UiTextService uiTextService;

    /**
     * UI 텍스트를 삭제한다.
     *
     * @param input 삭제할 UI 텍스트 key와 언어 입력
     */
    public void execute(UiTextKeyInput input) {
        uiTextService.deleteUiText(input.getKey(), input.getLanguage());
    }
}
