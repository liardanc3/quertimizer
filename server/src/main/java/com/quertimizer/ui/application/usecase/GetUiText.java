package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.port.UiTextRepository;
import com.quertimizer.ui.application.service.UiTextService;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUiText {

    private final UiTextRepository uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * 단일 UI 텍스트를 조회한다.
     *
     * @param input 조회할 UI 텍스트 key와 언어 입력
     */
    @Transactional(readOnly = true)
    public Optional<UiTextOutput> execute(UiTextKeyInput input) {
        String normalizedKey = uiTextService.normalizeKey(input.getKey());
        String normalizedLanguage = uiTextService.normalizeLanguage(input.getLanguage());

        return uiTextRepository.findByIdKeyAndIdLanguage(normalizedKey, normalizedLanguage)
                .or(() -> resolveDefaultText(normalizedKey, normalizedLanguage))
                .map(uiTextService::toOutput);
    }

    private Optional<UiText> resolveDefaultText(String key, String language) {
        // 기본 언어 요청 여부 검사
        if (UiTextLanguage.DEFAULT.getValue().equals(language)) {
            return Optional.empty();
        }

        // 요청 언어 텍스트 부재 시 기본 언어 텍스트 조회
        return uiTextRepository.findByIdKeyAndIdLanguage(key, UiTextLanguage.DEFAULT.getValue());
    }
}
