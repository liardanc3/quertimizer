package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;

public interface DeleteUiTextUseCase {

    void execute(UiTextKeyInput input);
}
