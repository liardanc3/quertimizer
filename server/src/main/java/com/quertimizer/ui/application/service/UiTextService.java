package com.quertimizer.ui.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import com.quertimizer.ui.domain.model.UiTextPageRules;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

import static com.quertimizer.ui.domain.model.UiTextFailReason.KEY_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.LANGUAGE_REQUIRED;

@Service
public class UiTextService {
    public UiTextId createRequiredUiTextId(String key, String language) {
        // 필수 key와 language 기준 UI 텍스트 ID 생성
        return createRequiredUiTextId(key, language, KEY_REQUIRED.getMessage(), LANGUAGE_REQUIRED.getMessage());
    }

    public UiTextId createRequiredUiTextId(String key, String language, String keyMessage, String languageMessage) {
        // 필수 key와 language 정규화 후 UI 텍스트 ID 생성
        return UiTextId.create(
                requireText(key, keyMessage).toUpperCase(Locale.ROOT),
                requireText(language, languageMessage).toLowerCase(Locale.ROOT)
        );
    }

    public String normalizeKey(String key) {
        // 선택 key 정규화
        return key != null ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    public String normalizeLanguage(String language) {
        // 선택 language 정규화와 기본 언어 대체
        return language != null && !language.isBlank()
                ? language.trim().toLowerCase(Locale.ROOT)
                : UiTextLanguage.DEFAULT.getValue();
    }

    public String normalizeSearchQuery(String query) {
        // 관리자 검색어 정규화
        return query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
    }

    public int resolveAdminUiTextPageSize(Integer requestedPageSize) {
        // 요청 페이지 크기 누락 또는 범위 미달 여부 검사
        if (requestedPageSize == null || requestedPageSize < 1) {
            return UiTextPageRules.ADMIN_PAGE_SIZE;
        }

        // 관리자 UI 텍스트 최대 페이지 크기 보정
        return Math.min(requestedPageSize, UiTextPageRules.ADMIN_MAX_PAGE_SIZE);
    }

    public boolean matchesAdminUiTextQuery(UiText uiText, String normalizedQuery) {
        // 빈 검색어 여부 검사
        if (normalizedQuery.isBlank()) {
            return true;
        }

        // key, value, description 검색어 포함 여부 확인
        return containsIgnoreCase(uiText.getKey(), normalizedQuery)
                || containsIgnoreCase(uiText.getValue(), normalizedQuery)
                || containsIgnoreCase(uiText.getDescription(), normalizedQuery);
    }

    public String requireText(String value, String message) {
        // 필수 문자열 null 또는 공백 여부 검사
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        // 필수 문자열 공백 제거 후 반환
        return value.trim();
    }

    public UiTextOutput toOutput(UiText uiText) {
        // UI 텍스트 응답 변환
        return new UiTextOutput(uiText.getKey(), uiText.getValue(), uiText.getLanguage(), uiText.getDescription());
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        // 대소문자 구분 없는 검색어 포함 여부 확인
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
