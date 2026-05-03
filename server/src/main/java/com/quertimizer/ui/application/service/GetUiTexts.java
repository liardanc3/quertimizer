package com.quertimizer.ui.application.service;

import com.quertimizer.ui.application.port.in.GetUiTextsUseCase;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.application.service.UiTextService;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetUiTexts implements GetUiTextsUseCase {

    private final UiTextRepositoryPort uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * 언어 기준 UI 텍스트 목록을 조회한다.
     *
     * @param language 조회할 UI 텍스트 언어
     */
    @Transactional(readOnly = true)
    @Override
    public List<UiTextOutput> execute(String language) {
        String normalizedLanguage = uiTextService.normalizeLanguage(language);
        Map<String, UiText> resolvedUiTexts = new LinkedHashMap<>();

        uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .filter(uiText -> normalizedLanguage.equals(uiText.getLanguage())
                        || UiTextLanguage.DEFAULT.getValue().equals(uiText.getLanguage()))
                .forEach(uiText -> putResolvedUiText(resolvedUiTexts, uiText));

        return resolvedUiTexts.values().stream()
                .map(uiTextService::toOutput)
                .toList();
    }

    private void putResolvedUiText(Map<String, UiText> resolvedUiTexts, UiText uiText) {
        // 기본 언어 텍스트 최초값 보관
        if (UiTextLanguage.DEFAULT.getValue().equals(uiText.getLanguage())) {
            resolvedUiTexts.putIfAbsent(uiText.getKey(), uiText);
            return;
        }

        // 요청 언어 텍스트로 기본 언어 텍스트 대체
        resolvedUiTexts.put(uiText.getKey(), uiText);
    }
}
