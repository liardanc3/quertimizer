package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetUiTexts {

    private final UiTextService uiTextService;

    public List<UiTextOutput> execute(String language) {
        // 언어 기준 UI 텍스트 목록을 조회
        return uiTextService.getUiTexts(language);
    }
}
