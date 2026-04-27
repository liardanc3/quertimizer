package com.quertimizer.ui.application.usecase;

import com.quertimizer.ui.application.output.UiTextPageOutput;
import com.quertimizer.ui.application.service.UiTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAdminUiTexts {

    private final UiTextService uiTextService;

    public UiTextPageOutput execute(int page, Integer pageSize, String query) {
        // 관리자 UI 텍스트 목록을 조회
        return uiTextService.getAdminUiTexts(page, pageSize, query);
    }
}
