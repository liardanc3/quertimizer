package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUiText {

    private final UiTextService uiTextService;

    public Optional<UiTextOutput> execute(String key, String language) {
        // 단일 UI 텍스트를 조회
        return uiTextService.getUiText(key, language);
    }
}
