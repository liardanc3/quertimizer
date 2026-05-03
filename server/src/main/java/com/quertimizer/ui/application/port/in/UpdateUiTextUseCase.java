package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.input.UpdateUiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;

public interface UpdateUiTextUseCase {

    UiTextOutput execute(UpdateUiTextInput input);
}
