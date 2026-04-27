package com.quertimizer.ui.presentation.dto.response;

import com.quertimizer.ui.application.output.UiTextPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UiTextPageRes {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<UiTextRes> uiTexts;

    public static UiTextPageRes from(UiTextPageOutput result) {
        return new UiTextPageRes(
                result.currentPage(),
                result.pageSize(),
                result.totalCount(),
                result.totalPages(),
                result.uiTexts().stream()
                        .map(UiTextRes::from)
                        .toList()
        );
    }

}
