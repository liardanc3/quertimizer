package com.quertimizer.ui.application.service;

import com.quertimizer.ui.presentation.dto.request.UiTextSaveReq;
import com.quertimizer.ui.presentation.dto.response.UiTextRes;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.UiTextId;
import com.quertimizer.ui.domain.model.UiTextKey;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.infrastructure.repository.UiTextRepository;
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

    public List<UiTextRes> getUiTexts(String language) {
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
                .map(UiTextRes::from)
                .toList();
    }

    public List<UiTextRes> getAdminUiTexts() {
        return uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .sorted(Comparator
                        .comparing((UiText uiText) -> !UiTextKey.NOTIFICATION.getValue().equals(uiText.getKey()))
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
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
        }

        UiText uiText = UiText.create(
                uiTextId.getKey(),
                requireText(request.getValue(), VALUE_REQUIRED.getMessage()),
                uiTextId.getLanguage(),
                requireText(request.getDescription(), DESCRIPTION_REQUIRED.getMessage())
        );

        return UiTextRes.from(uiTextRepository.save(uiText));
    }

    @Transactional
    public UiTextRes updateUiText(String originalKey, String originalLanguage, UiTextSaveReq request) {
        UiTextId originalUiTextId = createRequiredUiTextId(
                originalKey,
                originalLanguage,
                KEY_REQUIRED.getMessage(),
                LANGUAGE_REQUIRED.getMessage()
        );
        UiText existingUiText = uiTextRepository.findById(originalUiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        UiTextId nextUiTextId = UiTextId.create(
                normalizeRequiredKey(request.getKey()),
                normalizeRequiredLanguage(request.getLanguage())
        );
        String value = requireText(request.getValue(), VALUE_REQUIRED.getMessage());
        String description = requireText(request.getDescription(), DESCRIPTION_REQUIRED.getMessage());

        if (originalUiTextId.equals(nextUiTextId)) {
            existingUiText.changeContent(value, description);
            return UiTextRes.from(existingUiText);
        }

        if (uiTextRepository.existsById(nextUiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
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
                KEY_REQUIRED.getMessage(),
                LANGUAGE_REQUIRED.getMessage()
        );
        UiText uiText = uiTextRepository.findById(uiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        uiTextRepository.delete(uiText);
    }

    private Optional<UiText> resolveDefaultText(String key, String language) {
        if (UiTextLanguage.DEFAULT.getValue().equals(language)) {
            return Optional.empty();
        }

        return uiTextRepository.findByIdKeyAndIdLanguage(key, UiTextLanguage.DEFAULT.getValue());
    }

    private UiTextId createRequiredUiTextId(String key, String language, String keyMessage, String languageMessage) {
        return UiTextId.create(
                requireText(key, keyMessage).toUpperCase(Locale.ROOT),
                requireText(language, languageMessage).toLowerCase(Locale.ROOT)
        );
    }

    private String normalizeRequiredKey(String key) {
        return requireText(key, KEY_REQUIRED.getMessage()).toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredLanguage(String language) {
        return requireText(language, LANGUAGE_REQUIRED.getMessage()).toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        return key != null ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeLanguage(String language) {
        return language != null && !language.isBlank()
                ? language.trim().toLowerCase(Locale.ROOT)
                : UiTextLanguage.DEFAULT.getValue();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

}
