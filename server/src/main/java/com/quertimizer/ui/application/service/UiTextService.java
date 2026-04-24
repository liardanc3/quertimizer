package com.quertimizer.ui.application.service;

import com.quertimizer.ui.application.input.UiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.UiTextId;
import com.quertimizer.ui.domain.model.UiTextKey;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.application.port.UiTextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

import static com.quertimizer.ui.domain.model.UiTextFailReason.DESCRIPTION_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.DUPLICATED_UI_TEXT;
import static com.quertimizer.ui.domain.model.UiTextFailReason.KEY_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.LANGUAGE_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.UI_TEXT_NOT_FOUND;
import static com.quertimizer.ui.domain.model.UiTextFailReason.VALUE_REQUIRED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UiTextService {

    private final UiTextRepository uiTextRepository;

    public List<UiTextOutput> getUiTexts(String language) {
        // 언어 기준 UI 텍스트 목록을 조회
        String normalizedLanguage = normalizeLanguage(language);
        Map<String, UiText> resolvedUiTexts = new LinkedHashMap<>();

        uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .filter(uiText ->
                        normalizedLanguage.equals(uiText.getLanguage()) || UiTextLanguage.DEFAULT.getValue().equals(uiText.getLanguage()))
                .forEach(uiText -> {
                    if (UiTextLanguage.DEFAULT.getValue().equals(uiText.getLanguage())) {
                        resolvedUiTexts.putIfAbsent(uiText.getKey(), uiText);
                        return;
                    }

                    resolvedUiTexts.put(uiText.getKey(), uiText);
                });

        return resolvedUiTexts.values().stream()
                .map(this::toUiTextOutput)
                .toList();
    }

    public List<UiTextOutput> getAdminUiTexts() {
        // 관리자용 UI 텍스트 목록을 조회
        return uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .sorted(Comparator
                        .comparing((UiText uiText) -> !UiTextKey.NOTIFICATION.getValue().equals(uiText.getKey()))
                        .thenComparing(UiText::getKey)
                        .thenComparing(UiText::getLanguage))
                .map(this::toUiTextOutput)
                .toList();
    }

    public Optional<UiTextOutput> getUiText(String key, String language) {
        // 단일 UI 텍스트를 조회
        String normalizedKey = normalizeKey(key);
        String normalizedLanguage = normalizeLanguage(language);

        return uiTextRepository.findByIdKeyAndIdLanguage(normalizedKey, normalizedLanguage)
                .or(() -> resolveDefaultText(normalizedKey, normalizedLanguage))
                .map(this::toUiTextOutput);
    }

    @Transactional
    public UiTextOutput createUiText(UiTextInput input) {
        // 새 UI 텍스트를 생성
        UiTextId uiTextId = UiTextId.create(
                normalizeRequiredKey(input.getKey()),
                normalizeRequiredLanguage(input.getLanguage())
        );

        if (uiTextRepository.existsById(uiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
        }

        UiText uiText = UiText.create(
                uiTextId.getKey(),
                requireText(input.getValue(), VALUE_REQUIRED.getMessage()),
                uiTextId.getLanguage(),
                requireText(input.getDescription(), DESCRIPTION_REQUIRED.getMessage())
        );

        return toUiTextOutput(uiTextRepository.save(uiText));
    }

    @Transactional
    public UiTextOutput updateUiText(String originalKey, String originalLanguage, UiTextInput input) {
        // 기존 UI 텍스트를 수정
        UiTextId originalUiTextId = createRequiredUiTextId(
                originalKey,
                originalLanguage,
                KEY_REQUIRED.getMessage(),
                LANGUAGE_REQUIRED.getMessage()
        );
        UiText existingUiText = uiTextRepository.findById(originalUiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        UiTextId nextUiTextId = UiTextId.create(
                normalizeRequiredKey(input.getKey()),
                normalizeRequiredLanguage(input.getLanguage())
        );
        String value = requireText(input.getValue(), VALUE_REQUIRED.getMessage());
        String description = requireText(input.getDescription(), DESCRIPTION_REQUIRED.getMessage());

        if (originalUiTextId.equals(nextUiTextId)) {
            existingUiText.changeContent(value, description);
            return toUiTextOutput(existingUiText);
        }

        if (uiTextRepository.existsById(nextUiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
        }

        uiTextRepository.delete(existingUiText);
        return toUiTextOutput(uiTextRepository.save(UiText.create(
                nextUiTextId.getKey(),
                value,
                nextUiTextId.getLanguage(),
                description
        )));
    }

    @Transactional
    public void deleteUiText(String key, String language) {
        // UI 텍스트를 삭제
        UiTextId uiTextId = createRequiredUiTextId(
                key,
                language,
                KEY_REQUIRED.getMessage(),
                LANGUAGE_REQUIRED.getMessage()
        );
        UiText uiText = uiTextRepository.findById(uiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        uiTextRepository.delete(uiText);
    }

    private Optional<UiText> resolveDefaultText(String key, String language) {
        // 기본 텍스트 결정
        if (UiTextLanguage.DEFAULT.getValue().equals(language)) {
            return Optional.empty();
        }

        return uiTextRepository.findByIdKeyAndIdLanguage(key, UiTextLanguage.DEFAULT.getValue());
    }

    private UiTextId createRequiredUiTextId(String key, String language, String keyMessage, String languageMessage) {
        // 필수 UI 텍스트 번호 생성
        return UiTextId.create(
                requireText(key, keyMessage).toUpperCase(Locale.ROOT),
                requireText(language, languageMessage).toLowerCase(Locale.ROOT)
        );
    }

    private String normalizeRequiredKey(String key) {
        // 필수 키 정규화
        return requireText(key, KEY_REQUIRED.getMessage()).toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredLanguage(String language) {
        // 필수 언어 정규화
        return requireText(language, LANGUAGE_REQUIRED.getMessage()).toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        // 키 정규화
        return key != null ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeLanguage(String language) {
        // 언어 정규화
        return language != null && !language.isBlank()
                ? language.trim().toLowerCase(Locale.ROOT)
                : UiTextLanguage.DEFAULT.getValue();
    }

    private String requireText(String value, String message) {
        // 텍스트 필수값 검증
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    private UiTextOutput toUiTextOutput(UiText uiText) {
        // UI 텍스트 응답으로 변환
        return new UiTextOutput(
                uiText.getKey(),
                uiText.getValue(),
                uiText.getLanguage(),
                uiText.getDescription()
        );
    }
}
