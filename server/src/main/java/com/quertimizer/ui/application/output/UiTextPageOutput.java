package com.quertimizer.ui.application.output;

import java.util.List;

public record UiTextPageOutput(int currentPage,
                               int pageSize,
                               int totalCount,
                               int totalPages,
                               List<UiTextOutput> uiTexts) {
}
