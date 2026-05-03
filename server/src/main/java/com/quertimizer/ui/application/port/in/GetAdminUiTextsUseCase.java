package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.input.AdminUiTextSearchInput;
import com.quertimizer.ui.application.output.UiTextPageOutput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextKey;

public interface GetAdminUiTextsUseCase {

    UiTextPageOutput execute(AdminUiTextSearchInput input);
}
