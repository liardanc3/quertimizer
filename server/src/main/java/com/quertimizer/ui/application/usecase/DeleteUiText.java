package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteUiText {

    private final UiTextService uiTextService;

    public void execute(String key, String language) {
        // UI 텍스트를 삭제
        uiTextService.deleteUiText(key, language);
    }
}
