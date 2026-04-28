package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.input.AdminUiTextSearchInput;
import com.quertimizer.ui.application.output.UiTextPageOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAdminUiTexts {

    private final UiTextService uiTextService;

    /**
     * 관리자 UI 텍스트 목록을 조회한다.
     *
     * @param input 관리자 UI 텍스트 검색 입력
     */
    public UiTextPageOutput execute(AdminUiTextSearchInput input) {
        return uiTextService.getAdminUiTexts(input.getPage(), input.getPageSize(), input.getQuery());
    }
}
