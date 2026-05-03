package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import java.util.Optional;

public interface GetUiTextUseCase {

    Optional<UiTextOutput> execute(UiTextKeyInput input);
}
