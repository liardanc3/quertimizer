package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUiText {

    private final UiTextService uiTextService;

    /**
     * 단일 UI 텍스트를 조회한다.
     *
     * @param input 조회할 UI 텍스트 key와 언어 입력
     */
    public Optional<UiTextOutput> execute(UiTextKeyInput input) {
        return uiTextService.getUiText(input.getKey(), input.getLanguage());
    }
}
