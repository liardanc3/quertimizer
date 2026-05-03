package com.quertimizer.ui.application.port.in;

import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import java.util.List;

public interface GetUiTextsUseCase {

    List<UiTextOutput> execute(String language);
}
