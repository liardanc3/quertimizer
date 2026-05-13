package com.quertimizer.ui.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.ui.application.port.in.GetAdminUiTextsUseCase;
import com.quertimizer.ui.application.input.AdminUiTextSearchInput;
import com.quertimizer.ui.application.output.UiTextPageOutput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAdminUiTexts implements GetAdminUiTextsUseCase {

    private final UiTextRepositoryPort uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * 관리자 UI 텍스트 목록을 조회한다.
     *
     * @param input 관리자 UI 텍스트 검색 입력
     */
    @Transactional(readOnly = true)
    @Override
    @Log("관리자 UI 텍스트 조회")
    public UiTextPageOutput execute(AdminUiTextSearchInput input) {
        int pageSize = uiTextService.resolveAdminUiTextPageSize(input.getPageSize());
        String normalizedQuery = uiTextService.normalizeSearchQuery(input.getQuery());
        List<UiTextOutput> filteredUiTexts = uiTextRepository.findAllByOrderByIdKeyAscIdLanguageAsc().stream()
                .sorted(Comparator
                        .comparing((UiText uiText) -> !UiTextKey.NOTIFICATION.getValue().equals(uiText.getKey()))
                        .thenComparing(UiText::getKey)
                        .thenComparing(UiText::getLanguage))
                .filter(uiText -> uiTextService.matchesAdminUiTextQuery(uiText, normalizedQuery))
                .map(uiTextService::toOutput)
                .toList();
        int totalCount = filteredUiTexts.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.min(Math.max(input.getPage(), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        return new UiTextPageOutput(
                currentPage, pageSize, totalCount, totalPages,
                filteredUiTexts.subList(fromIndex, toIndex)
        );
    }
}
