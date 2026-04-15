package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.UiTextSaveReq;
import com.quertimizer.endpoint.api.dto.response.UiTextRes;
import com.quertimizer.entity.UiText;
import com.quertimizer.entity.UiTextId;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.UiTextRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UiTextService {

    private static final String DEFAULT_LANGUAGE = "default";
    private static final String NOTIFICATION_KEY = "NOTIFICATION";
    private static final String DUPLICATED_UI_TEXT_MESSAGE = "이미 존재하는 UI 텍스트다.";
    private static final String UI_TEXT_NOT_FOUND_MESSAGE = "존재하지 않는 UI 텍스트다.";
    private static final String VALUE_REQUIRED_MESSAGE = "값이 필요하다.";
    private static final String DESCRIPTION_REQUIRED_MESSAGE = "설명이 필요하다.";
    private static final String KEY_REQUIRED_MESSAGE = "key가 필요하다.";
    private static final String LANGUAGE_REQUIRED_MESSAGE = "language가 필요하다.";

    private final UiTextRepository uiTextRepository;

    public List<UiTextRes> getUiTexts(String language) {
        String normalizedLanguage = normalizeLanguage(language);
        Map<String, UiText> resolvedUiTexts = new LinkedHashMap<>();

        uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .filter(uiText ->
                        normalizedLanguage.equals(uiText.getLanguage()) || DEFAULT_LANGUAGE.equals(uiText.getLanguage()))
                .forEach(uiText -> {
                    if (DEFAULT_LANGUAGE.equals(uiText.getLanguage())) {
                        resolvedUiTexts.putIfAbsent(uiText.getKey(), uiText);
                        return;
                    }

                    resolvedUiTexts.put(uiText.getKey(), uiText);
                });

        return resolvedUiTexts.values().stream()
                .map(UiTextRes::from)
                .toList();
    }

    public List<UiTextRes> getAdminUiTexts() {
        return uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .sorted(Comparator
                        .comparing((UiText uiText) -> !NOTIFICATION_KEY.equals(uiText.getKey()))
                        .thenComparing(UiText::getKey)
                        .thenComparing(UiText::getLanguage))
                .map(UiTextRes::from)
                .toList();
    }

    public Optional<UiTextRes> getUiText(String key, String language) {
        String normalizedKey = normalizeKey(key);
        String normalizedLanguage = normalizeLanguage(language);

        return uiTextRepository.findByIdKeyAndIdLanguage(normalizedKey, normalizedLanguage)
                .or(() -> resolveDefaultText(normalizedKey, normalizedLanguage))
                .map(UiTextRes::from);
    }

    @Transactional
    public UiTextRes createUiText(UiTextSaveReq request) {
        UiTextId uiTextId = UiTextId.create(
                normalizeRequiredKey(request.getKey()),
                normalizeRequiredLanguage(request.getLanguage())
        );

        if (uiTextRepository.existsById(uiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT_MESSAGE, HttpStatus.CONFLICT);
        }

        UiText uiText = UiText.create(
                uiTextId.getKey(),
                requireText(request.getValue(), VALUE_REQUIRED_MESSAGE),
                uiTextId.getLanguage(),
                requireText(request.getDescription(), DESCRIPTION_REQUIRED_MESSAGE)
        );

        return UiTextRes.from(uiTextRepository.save(uiText));
    }

    @Transactional
    public UiTextRes updateUiText(String originalKey, String originalLanguage, UiTextSaveReq request) {
        UiTextId originalUiTextId = createRequiredUiTextId(
                originalKey,
                originalLanguage,
                KEY_REQUIRED_MESSAGE,
                LANGUAGE_REQUIRED_MESSAGE
        );
        UiText existingUiText = uiTextRepository.findById(originalUiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        UiTextId nextUiTextId = UiTextId.create(
                normalizeRequiredKey(request.getKey()),
                normalizeRequiredLanguage(request.getLanguage())
        );
        String value = requireText(request.getValue(), VALUE_REQUIRED_MESSAGE);
        String description = requireText(request.getDescription(), DESCRIPTION_REQUIRED_MESSAGE);

        if (originalUiTextId.equals(nextUiTextId)) {
            existingUiText.changeContent(value, description);
            return UiTextRes.from(existingUiText);
        }

        if (uiTextRepository.existsById(nextUiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT_MESSAGE, HttpStatus.CONFLICT);
        }

        uiTextRepository.delete(existingUiText);
        return UiTextRes.from(uiTextRepository.save(UiText.create(
                nextUiTextId.getKey(),
                value,
                nextUiTextId.getLanguage(),
                description
        )));
    }

    @Transactional
    public void deleteUiText(String key, String language) {
        UiTextId uiTextId = createRequiredUiTextId(
                key,
                language,
                KEY_REQUIRED_MESSAGE,
                LANGUAGE_REQUIRED_MESSAGE
        );
        UiText uiText = uiTextRepository.findById(uiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        uiTextRepository.delete(uiText);
    }

    private Optional<UiText> resolveDefaultText(String key, String language) {
        if (DEFAULT_LANGUAGE.equals(language)) {
            return Optional.empty();
        }

        return uiTextRepository.findByIdKeyAndIdLanguage(key, DEFAULT_LANGUAGE);
    }

    private UiTextId createRequiredUiTextId(String key, String language, String keyMessage, String languageMessage) {
        return UiTextId.create(
                requireText(key, keyMessage).toUpperCase(Locale.ROOT),
                requireText(language, languageMessage).toLowerCase(Locale.ROOT)
        );
    }

    private String normalizeRequiredKey(String key) {
        return requireText(key, KEY_REQUIRED_MESSAGE).toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredLanguage(String language) {
        return requireText(language, LANGUAGE_REQUIRED_MESSAGE).toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        return key != null ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeLanguage(String language) {
        return language != null && !language.isBlank()
                ? language.trim().toLowerCase(Locale.ROOT)
                : DEFAULT_LANGUAGE;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

}
