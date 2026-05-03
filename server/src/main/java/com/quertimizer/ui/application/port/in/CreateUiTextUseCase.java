package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.input.UiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;

public interface CreateUiTextUseCase {

    UiTextOutput execute(UiTextInput input);
}
